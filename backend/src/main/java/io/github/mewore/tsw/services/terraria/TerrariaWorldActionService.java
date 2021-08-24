package io.github.mewore.tsw.services.terraria;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.events.Subscription;
import io.github.mewore.tsw.models.terraria.world.TerrariaWorldEntity;
import io.github.mewore.tsw.services.LocalHostService;
import io.github.mewore.tsw.services.util.async.LifecycleThreadPool;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class TerrariaWorldActionService {

    private final Logger logger = LogManager.getLogger(getClass());

    private final TerrariaWorldApplicationEventService terrariaWorldApplicationEventService;

    private final LifecycleThreadPool lifecycleThreadPool;

    private final LocalHostService localHostService;

    private final TerrariaWorldFileService terrariaWorldFileService;

    @PostConstruct
    void setUp() {
        // Assuming that this service will last until the end of the application so there's no need to close the
        // subscription manually
        final Subscription<TerrariaWorldEntity> worldDeletionSubscription =
                terrariaWorldApplicationEventService.subscribeToWorldDeletions();
        lifecycleThreadPool.run(() -> waitForWorldDeletions(worldDeletionSubscription));
    }

    private void waitForWorldDeletions(final Subscription<TerrariaWorldEntity> subscription)
            throws InterruptedException {
        final TerrariaWorldEntity deletedWorld = subscription.take(
                candidate -> candidate.getHost().getUuid().equals(localHostService.getHostUuid()));
        logger.info("A local world has been deleted from the database: " + deletedWorld.getDisplayName());
        terrariaWorldFileService.deleteWorldFiles(deletedWorld);
    }
}
