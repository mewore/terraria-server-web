package io.github.mewore.tsw.services.terraria;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.events.Publisher;
import io.github.mewore.tsw.events.Subscription;
import io.github.mewore.tsw.events.TerrariaInstanceApplicationEvent;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class TerrariaInstanceSubscriptionService implements ApplicationListener<TerrariaInstanceApplicationEvent> {

    private final Logger logger = LogManager.getLogger(getClass());

    private final TerrariaInstanceRepository terrariaInstanceRepository;

    private final Publisher<Long, TerrariaInstanceEntity> instancePublisher;

    private final TerrariaInstanceMessageService terrariaInstanceMessageService;

    @PostConstruct
    void setUp() {
        instancePublisher.setTopicToValueMapper(terrariaInstanceRepository::getOne);
    }

    /**
     * Acknowledge an event that represents an updated or created Terraria instance entity.
     *
     * @param event The changed instance.
     * @deprecated This method is meant to be used only by Spring itself.
     */
    @Deprecated
    @Override
    public void onApplicationEvent(final TerrariaInstanceApplicationEvent event) {
        final TerrariaInstanceEntity instance = event.getChangedInstance();
        logger.debug("Application event for the {} of instance {}", event.isNew() ? "creation" : "deletion",
                instance.getId());
        instancePublisher.publish(instance.getId(), instance);

        if (event.isNew()) {
            terrariaInstanceMessageService.broadcastInstanceCreation(event.getChangedInstance());
        } else {
            terrariaInstanceMessageService.broadcastInstanceChange(event.getChangedInstance());
        }
    }

    public Subscription<TerrariaInstanceEntity> subscribe(final TerrariaInstanceEntity instance) {
        return instancePublisher.subscribe(instance.getId());
    }

    public Subscription<TerrariaInstanceEntity> subscribeToAll() {
        return instancePublisher.subscribe();
    }

    public TerrariaInstanceEntity waitForInstanceState(final TerrariaInstanceEntity instance,
            final Subscription<TerrariaInstanceEntity> subscription,
            final Duration timeout,
            final TerrariaInstanceState... desiredStates) throws IllegalStateException, InterruptedException {
        final String desiredStateString = Arrays.stream(desiredStates)
                .map(Objects::toString)
                .collect(Collectors.joining("/"));
        logger.debug("Waiting for instance {} to transition from {} to {}...", instance.getId(), instance.getState(),
                desiredStateString);
        final Set<TerrariaInstanceState> stateSet = Set.of(desiredStates);
        final @Nullable TerrariaInstanceEntity result = subscription.waitFor(
                newInstance -> stateSet.contains(newInstance.getState()), timeout);
        if (result == null) {
            throw new IllegalStateException(String.format(
                    "The instance %s did not reach the state(s) %s within a timeout of %s; instead, its state is %s.",
                    instance.getUuid(), desiredStateString, timeout, instance.getState()));
        }
        return result;
    }
}
