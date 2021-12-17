package io.github.mewore.tsw.services.terraria;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.core.type.TypeReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.events.Subscription;
import io.github.mewore.tsw.events.TerrariaInstanceApplicationEvent;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceRepository;
import io.github.mewore.tsw.services.database.DatabaseNotificationService;
import io.github.mewore.tsw.services.util.async.LifecycleThreadPool;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class TerrariaInstanceDbNotificationService {

    private static final String CREATION_CHANNEL_NAME = "terraria_instance_creations";

    private static final String UPDATE_CHANNEL_NAME = "terraria_instance_updates";

    private final Logger logger = LogManager.getLogger(getClass());

    private final DatabaseNotificationService databaseNotificationService;

    private final TerrariaInstanceRepository terrariaInstanceRepository;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final LifecycleThreadPool lifecycleThreadPool;

    @PostConstruct
    void setUp() {
        final Subscription<Long> subscriptionForCreation = databaseNotificationService.subscribe(CREATION_CHANNEL_NAME,
                new TypeReference<>() {
                });
        if (!subscriptionForCreation.canTake()) {
            return;
        }

        final Subscription<Long> subscriptionForUpdates = databaseNotificationService.subscribe(UPDATE_CHANNEL_NAME,
                new TypeReference<>() {
                });

        lifecycleThreadPool.run(() -> waitForInstanceNotification(subscriptionForCreation, true),
                () -> waitForInstanceNotification(subscriptionForUpdates, false));
    }

    public void instanceCreated(final @NonNull TerrariaInstanceEntity instance) {
        databaseNotificationService.trySend(CREATION_CHANNEL_NAME, instance.getId());
    }

    public void instanceUpdated(final @NonNull TerrariaInstanceEntity instance) {
        databaseNotificationService.trySend(UPDATE_CHANNEL_NAME, instance.getId());
    }

    private void waitForInstanceNotification(final Subscription<Long> notificationSubscription, final boolean created)
            throws InterruptedException {
        final Long instanceId = notificationSubscription.take();
        logger.info("Received a notification for the {} of the Terraria instance with ID {}",
                created ? "creation" : "update", instanceId);
        terrariaInstanceRepository.findById(instanceId)
                .ifPresentOrElse(instance -> applicationEventPublisher.publishEvent(
                                new TerrariaInstanceApplicationEvent(instance, created)),
                        () -> logger.warn("There is no instance with ID {}", instanceId));
    }
}
