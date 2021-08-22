package io.github.mewore.tsw.services.terraria;

import java.sql.SQLException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import io.github.mewore.tsw.events.FakeSubscription;
import io.github.mewore.tsw.events.Subscription;
import io.github.mewore.tsw.events.TerrariaInstanceApplicationEvent;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceRepository;
import io.github.mewore.tsw.services.database.DatabaseNotificationService;
import io.github.mewore.tsw.services.util.async.InterruptableRunnable;
import io.github.mewore.tsw.services.util.async.LifecycleThreadPool;

import static io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory.makeInstanceWithId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerrariaInstanceDbNotificationServiceTest {

    @InjectMocks
    private TerrariaInstanceDbNotificationService terrariaInstanceDbNotificationService;

    @Mock
    private DatabaseNotificationService databaseNotificationService;

    @Mock
    private TerrariaInstanceRepository terrariaInstanceRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private LifecycleThreadPool lifecycleThreadPool;

    @Mock
    private Subscription<String> creationSubscription;

    @Mock
    private Subscription<String> updateSubscription;

    @Captor
    private ArgumentCaptor<TerrariaInstanceApplicationEvent> instanceUpdatedEventCaptor;

    @Captor
    private ArgumentCaptor<InterruptableRunnable> creationThreadCaptor;

    @Captor
    private ArgumentCaptor<InterruptableRunnable> updateThreadCaptor;

    @Test
    void testWaitForInstanceNotification() throws InterruptedException {
        when(databaseNotificationService.subscribe("terraria_instance_creations")).thenReturn(
                new FakeSubscription<>("8"));

        terrariaInstanceDbNotificationService.setUp();
        verify(lifecycleThreadPool, only()).run(creationThreadCaptor.capture(), updateThreadCaptor.capture());
        final TerrariaInstanceEntity instance = mock(TerrariaInstanceEntity.class);
        when(terrariaInstanceRepository.findById(8L)).thenReturn(Optional.of(instance));

        creationThreadCaptor.getValue().run();
        verify(applicationEventPublisher, only()).publishEvent(instanceUpdatedEventCaptor.capture());
        assertSame(instance, instanceUpdatedEventCaptor.getValue().getChangedInstance());
    }

    @Test
    void testWaitForInstanceNotification_notFound() throws InterruptedException {
        when(databaseNotificationService.subscribe("terraria_instance_creations")).thenReturn(
                new FakeSubscription<>("8"));
        terrariaInstanceDbNotificationService.setUp();
        verify(lifecycleThreadPool, only()).run(creationThreadCaptor.capture(), updateThreadCaptor.capture());
        when(terrariaInstanceRepository.findById(8L)).thenReturn(Optional.empty());

        creationThreadCaptor.getValue().run();
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void testWaitForInstanceNotification_invalidId() throws InterruptedException {
        when(databaseNotificationService.subscribe("terraria_instance_creations")).thenReturn(
                new FakeSubscription<>("not-a-number"));
        terrariaInstanceDbNotificationService.setUp();

        verify(lifecycleThreadPool, only()).run(creationThreadCaptor.capture(), updateThreadCaptor.capture());

        creationThreadCaptor.getValue().run();
        verify(terrariaInstanceRepository, never()).findById(anyLong());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void testWaitForInstanceNotification_update() throws InterruptedException {
        when(databaseNotificationService.subscribe("terraria_instance_creations")).thenReturn(null);
        when(databaseNotificationService.subscribe("terraria_instance_updates")).thenReturn(
                new FakeSubscription<>("8"));

        terrariaInstanceDbNotificationService.setUp();
        verify(lifecycleThreadPool, only()).run(creationThreadCaptor.capture(), updateThreadCaptor.capture());
        final TerrariaInstanceEntity instance = mock(TerrariaInstanceEntity.class);
        when(terrariaInstanceRepository.findById(8L)).thenReturn(Optional.of(instance));

        updateThreadCaptor.getValue().run();
        verify(applicationEventPublisher, only()).publishEvent(instanceUpdatedEventCaptor.capture());
        assertSame(instance, instanceUpdatedEventCaptor.getValue().getChangedInstance());
    }

    @Test
    void testWaitForInstanceNotification_closedSubscription() {
        when(databaseNotificationService.subscribe("terraria_instance_creations")).thenReturn(creationSubscription);
        when(creationSubscription.isOpen()).thenReturn(false);
        when(databaseNotificationService.subscribe("terraria_instance_updates")).thenReturn(updateSubscription);

        terrariaInstanceDbNotificationService.setUp();
        verify(lifecycleThreadPool, only()).run(creationThreadCaptor.capture(), updateThreadCaptor.capture());

        final Exception exception = assertThrows(InterruptedException.class,
                () -> creationThreadCaptor.getValue().run());
        assertEquals("The DB notification subscription for channel terraria_instance_creations is closed",
                exception.getMessage());
    }

    @Test
    void testSendNotification_created() throws SQLException {
        terrariaInstanceDbNotificationService.onInstanceCreated(makeInstanceWithId(1L));
        verify(databaseNotificationService, only()).send("terraria_instance_creations", "1");
    }

    @Test
    void testSendNotification_created_error() throws SQLException {
        doThrow(new SQLException("oof")).when(databaseNotificationService).send("terraria_instance_creations", "1");
        terrariaInstanceDbNotificationService.onInstanceCreated(makeInstanceWithId(1L));
    }

    @Test
    void testSendNotification_updated() throws SQLException {
        terrariaInstanceDbNotificationService.onInstanceUpdated(makeInstanceWithId(1L));
        verify(databaseNotificationService, only()).send("terraria_instance_updates", "1");
    }
}