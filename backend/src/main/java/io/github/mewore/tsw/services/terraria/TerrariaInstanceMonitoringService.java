package io.github.mewore.tsw.services.terraria;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.exceptions.InvalidInstanceException;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceAction;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceRepository;
import io.github.mewore.tsw.services.LocalHostService;
import io.github.mewore.tsw.services.util.AsyncService;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Service
public class TerrariaInstanceMonitoringService {

    private static final Duration POLL_RATE = Duration.ofSeconds(10);

    private final Logger logger = LogManager.getLogger(getClass());

    private final @NonNull LocalHostService localHostService;

    private final @NonNull TerrariaInstanceService terrariaInstanceService;

    private final @NonNull TerrariaInstanceRepository terrariaInstanceRepository;

    private final @NonNull AsyncService asyncService;

    @PostConstruct
    void setUp() {
        asyncService.scheduleAtFixedRate(this::checkInstances, Duration.ZERO, POLL_RATE);
    }

    private void checkInstances() {
        terrariaInstanceRepository.findOneByHostAndPendingActionNotNull(localHostService.getOrCreateHost())
                .ifPresent(this::updateInstance);
    }

    private void updateInstance(final TerrariaInstanceEntity instance) {
        final TerrariaInstanceAction action = instance.getPendingAction();
        if (action == null) {
            logger.warn("Tried to update the Terraria instance {} which has no pending action!", instance.getUuid());
            return;
        }

        try {
            final TerrariaInstanceEntity instanceWithStartTime =
                    terrariaInstanceRepository.save(instance.withActionExecutionStartTime(Instant.now()));
            final TerrariaInstanceEntity updated;
            // SET_UP is the only possible action at the moment
            updated = terrariaInstanceService.setUpTerrariaInstance(instanceWithStartTime);
            logger.info("Updated Terraria instance {}: [{}] -[{}]-> [{}]", instance.getUuid(), instance.getState(),
                    instance.getPendingAction(), updated.getState());
        } catch (final InvalidInstanceException e) {
            logger.info("Marking Terraria instance " + instance.getUuid() + " as invalid", e);
            terrariaInstanceRepository.save(instance.toBuilder()
                    .pendingAction(null)
                    .actionExecutionStartTime(null)
                    .state(TerrariaInstanceState.INVALID)
                    .error(e.getMessage())
                    .build());
        } catch (final IOException | RuntimeException e) {
            logger.error("Marking Terraria instance " + instance.getUuid() + " as broken", e);
            terrariaInstanceRepository.save(instance.toBuilder()
                    .pendingAction(null)
                    .actionExecutionStartTime(null)
                    .state(TerrariaInstanceState.BROKEN)
                    .error(e.getMessage())
                    .build());
        }
    }
}
