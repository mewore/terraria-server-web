package io.github.mewore.tsw.services.terraria;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import io.github.mewore.tsw.events.DeletedWorldNotification;
import io.github.mewore.tsw.events.FakeSubscription;
import io.github.mewore.tsw.events.NullSubscription;
import io.github.mewore.tsw.events.TerrariaWorldDeletionEvent;
import io.github.mewore.tsw.models.HostFactory;
import io.github.mewore.tsw.services.database.DatabaseNotificationService;
import io.github.mewore.tsw.services.util.async.InterruptableRunnable;
import io.github.mewore.tsw.services.util.async.LifecycleThreadPool;

import static io.github.mewore.tsw.models.terraria.TerrariaWorldFactory.makeWorld;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    @Captor
    private ArgumentCaptor<DeletedWorldNotification> deletedWorldCaptor;

    @Test
    void testSetUp_nullSubscription() {
        when(databaseNotificationService.subscribe(eq("terraria_world_deletions"), any())).thenReturn(
                new NullSubscription<>());

        terrariaWorldDbNotificationService.setUp();
        verify(lifecycleThreadPool, never()).run(any());
    }

    @Test
    void testWaitForWorldDeletionNotification() throws InterruptedException {
        final DeletedWorldNotification world = mock(DeletedWorldNotification.class);
        when(databaseNotificationService.subscribe(eq("terraria_world_deletions"), any())).thenReturn(
                new FakeSubscription<>(world));

        terrariaWorldDbNotificationService.setUp();
        verify(lifecycleThreadPool, only()).run(deletionThreadCaptor.capture());

        deletionThreadCaptor.getValue().run();
        verify(applicationEventPublisher, only()).publishEvent(worldDeletedEventCaptor.capture());
        assertSame(world, worldDeletedEventCaptor.getValue().getDeletedWorld());
    }

    @Test
    void testWorldDeleted() {
        terrariaWorldDbNotificationService.worldDeleted(makeWorld());

        verify(databaseNotificationService, only()).trySend(eq("terraria_world_deletions"),
                deletedWorldCaptor.capture());
        assertEquals(1L, deletedWorldCaptor.getValue().getId());
        assertEquals("World_Name", deletedWorldCaptor.getValue().getFileName());
        assertEquals("World Name", deletedWorldCaptor.getValue().getDisplayName());
        assertSame(HostFactory.HOST_UUID, deletedWorldCaptor.getValue().getHostUuid());
    }
}