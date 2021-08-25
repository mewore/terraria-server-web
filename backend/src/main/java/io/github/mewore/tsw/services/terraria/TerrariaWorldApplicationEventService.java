package io.github.mewore.tsw.services.terraria;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.events.DeletedWorldNotification;
import io.github.mewore.tsw.events.Publisher;
import io.github.mewore.tsw.events.Subscription;
import io.github.mewore.tsw.events.TerrariaWorldDeletionEvent;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class TerrariaWorldApplicationEventService {

    private final Logger logger = LogManager.getLogger(getClass());

    private final Publisher<Long, DeletedWorldNotification> worldDeletionPublisher;

    private final TerrariaWorldMessageService terrariaWorldMessageService;

    /**
     * Acknowledge an event of the deletion of a world.
     *
     * @param event The deleted world.
     * @deprecated This method is meant to be used only by Spring itself.
     */
    @Deprecated
    @EventListener
    void onWorldDeleted(final TerrariaWorldDeletionEvent event) {
        logger.info("A world has been deleted: " + event.getDeletedWorld().getId());
        worldDeletionPublisher.publish(event.getDeletedWorld().getId(), event.getDeletedWorld());
        terrariaWorldMessageService.broadcastWorldDeletion(event.getDeletedWorld());
    }

    public Subscription<DeletedWorldNotification> subscribeToWorldDeletions() {
        return worldDeletionPublisher.subscribe();
    }
}
