package io.github.mewore.tsw.services.terraria;

import javax.annotation.PostConstruct;
import java.sql.SQLException;

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
        final Subscription<String> subscriptionForCreation = databaseNotificationService.subscribe(
                CREATION_CHANNEL_NAME);
        final Subscription<String> subscriptionForUpdates = databaseNotificationService.subscribe(UPDATE_CHANNEL_NAME);

        lifecycleThreadPool.run(() -> waitForInstanceNotification(subscriptionForCreation, CREATION_CHANNEL_NAME, true),
                () -> waitForInstanceNotification(subscriptionForUpdates, UPDATE_CHANNEL_NAME, false));
    }

    public void onInstanceCreated(final @NonNull TerrariaInstanceEntity instance) {
        trySendNotification(CREATION_CHANNEL_NAME, instance);
    }

    public void onInstanceUpdated(final @NonNull TerrariaInstanceEntity instance) {
        trySendNotification(UPDATE_CHANNEL_NAME, instance);
    }

    private void trySendNotification(final @NonNull String channel, final @NonNull TerrariaInstanceEntity instance) {
        try {
            databaseNotificationService.send(channel, instance.getId().toString());
        } catch (final SQLException e) {
            logger.error("Failed to send a notification to channel \"" + channel + "\"", e);
        }
    }

    private void waitForInstanceNotification(final Subscription<String> notificationSubscription,
            final String channelName,
            final boolean created) throws InterruptedException {
        if (!notificationSubscription.isOpen()) {
            throw new InterruptedException(
                    "The DB notification subscription for channel " + channelName + " is closed");
        }
        final String notification = notificationSubscription.take();
        final long instanceId;
        try {
            instanceId = Long.parseLong(notification);
        } catch (final NumberFormatException e) {
            logger.error("The notification \"" + notification + "\" is not a valid long number", e);
            return;
        }
        logger.info("Received notification \"{}\" for the Terraria instance with ID {}", notification, instanceId);
        terrariaInstanceRepository.findById(instanceId)
                .ifPresentOrElse(instance -> applicationEventPublisher.publishEvent(
                                new TerrariaInstanceApplicationEvent(instance, created)),
                        () -> logger.warn("There is no instance with ID {}", instanceId));
    }
}
