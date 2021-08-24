package io.github.mewore.tsw.services.terraria;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;

import io.github.mewore.tsw.config.TestConfig;
import io.github.mewore.tsw.events.Subscription;
import io.github.mewore.tsw.events.TerrariaWorldDeletionEvent;
import io.github.mewore.tsw.models.terraria.world.TerrariaWorldEntity;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import(TestConfig.class)
@SpringBootTest
class TerrariaWorldApplicationEventServiceIT {

    private static final long WORLD_ID = 1;

    @Autowired
    private TerrariaWorldApplicationEventService terrariaWorldApplicationEventService;

    @MockBean
    private TerrariaWorldMessageService terrariaWorldMessageService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Test
    void testOnWorldDeleted() throws InterruptedException {
        try (final Subscription<TerrariaWorldEntity> subscription =
                     terrariaWorldApplicationEventService.subscribeToWorldDeletions()) {
            final TerrariaWorldEntity world = mock(TerrariaWorldEntity.class);
            when(world.getId()).thenReturn(WORLD_ID);
            eventPublisher.publishEvent(new TerrariaWorldDeletionEvent(world));
            assertSame(world, subscription.waitFor(unused -> true, Duration.ZERO));
            verify(terrariaWorldMessageService, only()).broadcastWorldDeletion(world);
        }
    }
}