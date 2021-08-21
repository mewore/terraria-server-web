package io.github.mewore.tsw.services.terraria;

import javax.annotation.PostConstruct;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.events.Subscription;
import io.github.mewore.tsw.events.TerrariaInstanceUpdatedEvent;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceRepository;
import io.github.mewore.tsw.services.database.DatabaseNotificationService;
import io.github.mewore.tsw.services.util.async.LifecycleThreadPool;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class TerrariaInstanceDbNotificationService {

    private static final String CHANNEL_NAME = "terraria_instances";

    private final Logger logger = LogManager.getLogger(getClass());

    private final DatabaseNotificationService databaseNotificationService;

    private final TerrariaInstanceRepository terrariaInstanceRepository;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final LifecycleThreadPool lifecycleThreadPool;

    @PostConstruct
    void setUp() throws SQLException {
        final Subscription<String> notificationSubscription = databaseNotificationService.subscribe(CHANNEL_NAME);
        lifecycleThreadPool.run(() -> waitForInstanceNotification(notificationSubscription));
    }

    public void sendNotification(final TerrariaInstanceEntity instance) {
        try {
            databaseNotificationService.send(CHANNEL_NAME, instance.getId().toString());
        } catch (final SQLException e) {
            logger.error("Failed to send a notification to channel \"" + CHANNEL_NAME + "\"", e);
        }
    }

    private void waitForInstanceNotification(final Subscription<String> notificationSubscription)
            throws InterruptedException {
        if (!notificationSubscription.isOpen()) {
            throw new InterruptedException(
                    "The DB notification subscription for channel " + CHANNEL_NAME + " is closed");
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
                .ifPresentOrElse(
                        instance -> applicationEventPublisher.publishEvent(new TerrariaInstanceUpdatedEvent(instance)),
                        () -> logger.warn("There is no instance with ID {}", instanceId));
    }
}
