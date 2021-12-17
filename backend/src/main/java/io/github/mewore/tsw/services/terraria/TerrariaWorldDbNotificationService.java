package io.github.mewore.tsw.services.terraria;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.core.type.TypeReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.events.DeletedWorldNotification;
import io.github.mewore.tsw.events.Subscription;
import io.github.mewore.tsw.events.TerrariaWorldDeletionEvent;
import io.github.mewore.tsw.models.terraria.world.TerrariaWorldEntity;
import io.github.mewore.tsw.services.database.DatabaseNotificationService;
import io.github.mewore.tsw.services.util.async.LifecycleThreadPool;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class TerrariaWorldDbNotificationService {

    private static final String DELETION_CHANNEL_NAME = "terraria_world_deletions";

    private final Logger logger = LogManager.getLogger(getClass());

    private final DatabaseNotificationService databaseNotificationService;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final LifecycleThreadPool lifecycleThreadPool;

    @PostConstruct
    void setUp() {
        final Subscription<DeletedWorldNotification> subscriptionForDeletion = databaseNotificationService.subscribe(
                DELETION_CHANNEL_NAME, new TypeReference<>() {
                });

        if (subscriptionForDeletion.canTake()) {
            lifecycleThreadPool.run(() -> waitForWorldDeletionNotification(subscriptionForDeletion));
        }
    }

    public void worldDeleted(final @NonNull TerrariaWorldEntity world) {
        databaseNotificationService.trySend(DELETION_CHANNEL_NAME, new DeletedWorldNotification(world));
    }

    private void waitForWorldDeletionNotification(final Subscription<DeletedWorldNotification> notificationSubscription)
            throws InterruptedException {
        final DeletedWorldNotification deletedWorld = notificationSubscription.take();
        logger.info("Received a notification for the deletion of the world \"{}\" with ID {}",
                deletedWorld.getDisplayName(), deletedWorld.getId());
        applicationEventPublisher.publishEvent(new TerrariaWorldDeletionEvent(deletedWorld));
    }
}
