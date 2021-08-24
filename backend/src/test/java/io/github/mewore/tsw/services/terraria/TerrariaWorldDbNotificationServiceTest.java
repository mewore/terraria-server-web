package io.github.mewore.tsw.services.terraria;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import io.github.mewore.tsw.events.FakeSubscription;
import io.github.mewore.tsw.events.TerrariaWorldDeletionEvent;
import io.github.mewore.tsw.models.terraria.world.TerrariaWorldEntity;
import io.github.mewore.tsw.services.database.DatabaseNotificationService;
import io.github.mewore.tsw.services.util.async.InterruptableRunnable;
import io.github.mewore.tsw.services.util.async.LifecycleThreadPool;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerrariaWorldDbNotificationServiceTest {

    @InjectMocks
    private TerrariaWorldDbNotificationService terrariaWorldDbNotificationService;

    @Mock
    private DatabaseNotificationService databaseNotificationService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private LifecycleThreadPool lifecycleThreadPool;

    @Captor
    private ArgumentCaptor<TerrariaWorldDeletionEvent> worldDeletedEventCaptor;

    @Captor
    private ArgumentCaptor<InterruptableRunnable> deletionThreadCaptor;

    @Test
    void testWaitForWorldDeletionNotification() throws InterruptedException {
        final TerrariaWorldEntity world = mock(TerrariaWorldEntity.class);
        when(databaseNotificationService.subscribe("terraria_world_deletions")).thenReturn(
                new FakeSubscription<>(world));

        terrariaWorldDbNotificationService.setUp();
        verify(lifecycleThreadPool, only()).run(deletionThreadCaptor.capture());

        deletionThreadCaptor.getValue().run();
        verify(applicationEventPublisher, only()).publishEvent(worldDeletedEventCaptor.capture());
        assertSame(world, worldDeletedEventCaptor.getValue().getDeletedWorld());
    }

    @Test
    void testWorldDeleted() {
        final TerrariaWorldEntity world = mock(TerrariaWorldEntity.class);
        terrariaWorldDbNotificationService.worldDeleted(world);
        verify(databaseNotificationService, only()).trySend("terraria_world_deletions", world);
    }
}