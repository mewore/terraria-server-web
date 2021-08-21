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
import io.github.mewore.tsw.events.TerrariaInstanceUpdatedEvent;
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
    private Subscription<String> subscription;

    @Captor
    private ArgumentCaptor<TerrariaInstanceUpdatedEvent> instanceUpdatedEventCaptor;

    @Captor
    private ArgumentCaptor<InterruptableRunnable> runnableCaptor;

    @Test
    void testWaitForInstanceNotification() throws SQLException, InterruptedException {
        when(databaseNotificationService.subscribe("terraria_instances")).thenReturn(new FakeSubscription<>("8"));

        terrariaInstanceDbNotificationService.setUp();
        verify(lifecycleThreadPool, only()).run(runnableCaptor.capture());
        final TerrariaInstanceEntity instance = mock(TerrariaInstanceEntity.class);
        when(terrariaInstanceRepository.findById(8L)).thenReturn(Optional.of(instance));

        runnableCaptor.getValue().run();
        verify(applicationEventPublisher, only()).publishEvent(instanceUpdatedEventCaptor.capture());
        assertSame(instance, instanceUpdatedEventCaptor.getValue().getChangedInstance());
    }

    @Test
    void testWaitForInstanceNotification_notFound() throws SQLException, InterruptedException {
        when(databaseNotificationService.subscribe("terraria_instances")).thenReturn(new FakeSubscription<>("8"));
        terrariaInstanceDbNotificationService.setUp();
        verify(lifecycleThreadPool, only()).run(runnableCaptor.capture());
        when(terrariaInstanceRepository.findById(8L)).thenReturn(Optional.empty());

        runnableCaptor.getValue().run();
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void testWaitForInstanceNotification_invalidId() throws SQLException, InterruptedException {
        when(databaseNotificationService.subscribe("terraria_instances")).thenReturn(
                new FakeSubscription<>("not-a-number"));
        terrariaInstanceDbNotificationService.setUp();

        verify(lifecycleThreadPool, only()).run(runnableCaptor.capture());

        runnableCaptor.getValue().run();
        verify(terrariaInstanceRepository, never()).findById(anyLong());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void testWaitForInstanceNotification_closedSubscription() throws SQLException {
        when(databaseNotificationService.subscribe("terraria_instances")).thenReturn(subscription);
        when(subscription.isOpen()).thenReturn(false);

        terrariaInstanceDbNotificationService.setUp();
        verify(lifecycleThreadPool, only()).run(runnableCaptor.capture());

        final Exception exception = assertThrows(InterruptedException.class, () -> runnableCaptor.getValue().run());
        assertEquals("The DB notification subscription for channel terraria_instances is closed",
                exception.getMessage());
    }

    @Test
    void testSendNotification() throws SQLException {
        terrariaInstanceDbNotificationService.sendNotification(makeInstanceWithId(1L));
        verify(databaseNotificationService, only()).send("terraria_instances", "1");
    }

    @Test
    void testSendNotification_error() throws SQLException {
        doThrow(new SQLException("oof")).when(databaseNotificationService).send("terraria_instances", "1");
        terrariaInstanceDbNotificationService.sendNotification(makeInstanceWithId(1L));
    }
}