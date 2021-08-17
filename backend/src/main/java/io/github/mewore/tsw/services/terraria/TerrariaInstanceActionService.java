package io.github.mewore.tsw.services.terraria;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.events.Subscription;
import io.github.mewore.tsw.exceptions.InvalidInstanceException;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceAction;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventType;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceRepository;
import io.github.mewore.tsw.services.LocalHostService;
import io.github.mewore.tsw.services.util.AsyncService;
import io.github.mewore.tsw.services.util.process.ProcessFailureException;
import io.github.mewore.tsw.services.util.process.ProcessTimeoutException;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Service
public class TerrariaInstanceActionService {

    private static final Duration POLL_TIME = Duration.ofSeconds(60);

    private final Logger logger = LogManager.getLogger(getClass());

    private final @NonNull LocalHostService localHostService;

    private final @NonNull TerrariaInstanceService terrariaInstanceService;

    private final @NonNull TerrariaInstancePreparationService terrariaInstancePreparationService;

    private final @NonNull TerrariaInstanceExecutionService terrariaInstanceExecutionService;

    private final @NonNull TerrariaInstanceOutputService terrariaInstanceOutputService;

    private final @NonNull TerrariaInstanceSubscriptionService terrariaInstanceSubscriptionService;

    private final @NonNull TerrariaInstanceRepository terrariaInstanceRepository;

    private final @NonNull AsyncService asyncService;

    @PostConstruct
    void setUp() {
        final List<TerrariaInstanceEntity> allLocalInstances = terrariaInstanceRepository.findByHostUuid(
                localHostService.getHostUuid());
        for (final TerrariaInstanceEntity instance : allLocalInstances) {
            if (instance.getState().isActive() && instance.getOutputFile().exists()) {
                terrariaInstanceOutputService.trackInstance(instance);
            }
        }

        asyncService.runContinuously(this::checkInstances);
    }

    private void checkInstances() throws InterruptedException {
        Optional.ofNullable(waitForUpdatableInstance()).ifPresent(this::updateInstance);
    }

    private @Nullable TerrariaInstanceEntity waitForUpdatableInstance() throws InterruptedException {
        final UUID hostUuid = localHostService.getHostUuid();
        final @Nullable TerrariaInstanceEntity initialInstance =
                terrariaInstanceRepository.findTopByHostUuidAndPendingActionNotNull(
                hostUuid).orElse(null);
        if (initialInstance != null) {
            return initialInstance;
        }

        try (final Subscription<TerrariaInstanceEntity> subscription =
                     terrariaInstanceSubscriptionService.subscribeToAll()) {
            return subscription.waitFor(
                    instance -> instance.getHost().getUuid().equals(hostUuid) && instance.getPendingAction() != null,
                    POLL_TIME);
        }
    }

    private void updateInstance(TerrariaInstanceEntity instance) {
        final TerrariaInstanceState originalState = instance.getState();
        final TerrariaInstanceAction action = instance.getPendingAction();
        if (action == null) {
            logger.warn("Tried to update the Terraria instance {} which has no pending action!", instance.getUuid());
            return;
        }
        instance.setError(null);
        if (action.isInapplicableTo(originalState)) {
            logger.error(
                    "Cannot apply action [{}] to an instance with state [{}]! Removing the action. The only allowed " +
                            "actions for the [{}] state are: {}", action, originalState, originalState,
                    String.join(", ", originalState.getApplicableActionNames()));
            instance.setPendingAction(null);
            terrariaInstanceService.saveInstance(instance);
            return;
        }

        try {
            instance.startAction();
            instance = terrariaInstanceService.saveInstance(instance);
            final TerrariaInstanceEntity newInstance = applyAction(instance, action);
            if (newInstance == null) {
                logger.info("Updated Terraria instance {}: [{}] -[{}]-> [DELETED]", instance.getUuid(), originalState,
                        action);
                return;
            }
            instance = newInstance;
            instance.completeAction();
            instance.setError(null);
            instance = terrariaInstanceService.saveInstance(instance);
            logger.info("Updated Terraria instance {}: [{}] -[{}]-> [{}]", instance.getUuid(), originalState, action,
                    instance.getState());
        } catch (final InvalidInstanceException e) {
            applyExceptionToInstance(instance, e, TerrariaInstanceState.INVALID,
                    TerrariaInstanceEventType.INVALID_INSTANCE);
        } catch (final IllegalArgumentException e) {
            applyExceptionToInstance(instance, e, instance.getState(), TerrariaInstanceEventType.ERROR);
        } catch (final RuntimeException e) {
            applyExceptionToInstance(instance, e, TerrariaInstanceState.BROKEN, TerrariaInstanceEventType.ERROR);
        } catch (final InterruptedException e) {
            applyExceptionToInstance(instance, e, TerrariaInstanceState.BROKEN,
                    TerrariaInstanceEventType.TSW_INTERRUPTED);
            logger.warn("Interrupted while applying an action to instance " + instance.getUuid(), e);
            Thread.currentThread().interrupt();
        }
    }

    @SuppressWarnings("OverlyComplexMethod")
    private @Nullable TerrariaInstanceEntity applyAction(final TerrariaInstanceEntity instance,
            final TerrariaInstanceAction action) throws InvalidInstanceException, InterruptedException {
        try {
            switch (action) {
                case SET_UP: {
                    return terrariaInstancePreparationService.setUpInstance(instance);
                }
                case BOOT_UP: {
                    return terrariaInstanceExecutionService.bootUpInstance(instance);
                }
                case GO_TO_MOD_MENU: {
                    return terrariaInstanceExecutionService.goToModMenu(instance);
                }
                case SET_LOADED_MODS: {
                    return terrariaInstanceExecutionService.setInstanceLoadedMods(instance);
                }
                case CREATE_WORLD: {
                    return terrariaInstanceExecutionService.createWorld(instance);
                }
                case RUN_SERVER: {
                    return terrariaInstanceExecutionService.runInstance(instance);
                }
                case SHUT_DOWN: {
                    return terrariaInstanceExecutionService.shutDownInstance(instance, true);
                }
                case SHUT_DOWN_NO_SAVE: {
                    return terrariaInstanceExecutionService.shutDownInstance(instance, false);
                }
                case TERMINATE: {
                    return terrariaInstanceExecutionService.terminateInstance(instance);
                }
                case RECREATE: {
                    return terrariaInstanceExecutionService.recreateInstance(instance);
                }
                case DELETE: {
                    terrariaInstanceExecutionService.deleteInstance(instance);
                    return null;
                }
                default: {
                    throw new UnsupportedOperationException("Cannot handle action [" + action + "]");
                }
            }
        } catch (final IOException | ProcessFailureException | ProcessTimeoutException e) {
            // Cannot handle these exceptions in any meaningful way
            throw new RuntimeException(e);
        }
    }

    @Transactional
    private void applyExceptionToInstance(final TerrariaInstanceEntity instance,
            final Exception exception,
            final TerrariaInstanceState newState,
            final TerrariaInstanceEventType eventType) {
        logger.warn("Marking Terraria instance " + instance.getUuid() + " as " + newState, exception);

        final @Nullable String exceptionMessage = exception.getMessage();
        final TerrariaInstanceEventEntity event = TerrariaInstanceEventEntity.builder()
                .type(eventType)
                .content(exceptionMessage == null ? exception.getClass().getSimpleName() : exceptionMessage)
                .instance(instance)
                .build();

        instance.completeAction();
        instance.setState(newState);
        instance.setError(event.getContent());

        terrariaInstanceService.saveInstanceAndEvent(instance, event);
    }
}
