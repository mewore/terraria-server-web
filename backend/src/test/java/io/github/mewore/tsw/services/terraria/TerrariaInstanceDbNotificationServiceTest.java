package io.github.mewore.tsw.services.terraria;

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
import io.github.mewore.tsw.events.TerrariaInstanceApplicationEvent;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceRepository;
import io.github.mewore.tsw.services.database.DatabaseNotificationService;
import io.github.mewore.tsw.services.util.async.InterruptableRunnable;
import io.github.mewore.tsw.services.util.async.LifecycleThreadPool;

import static io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory.makeInstanceWithId;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
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

    @Captor
    private ArgumentCaptor<TerrariaInstanceApplicationEvent> instanceUpdatedEventCaptor;

    @Captor
    private ArgumentCaptor<InterruptableRunnable> creationThreadCaptor;

    @Captor
    private ArgumentCaptor<InterruptableRunnable> updateThreadCaptor;

    @Test
    void testWaitForInstanceNotification() throws InterruptedException {
        when(databaseNotificationService.subscribe("terraria_instance_creations")).thenReturn(
                new FakeSubscription<>(8L));

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
                new FakeSubscription<>(8L));
        terrariaInstanceDbNotificationService.setUp();
        verify(lifecycleThreadPool, only()).run(creationThreadCaptor.capture(), updateThreadCaptor.capture());
        when(terrariaInstanceRepository.findById(8L)).thenReturn(Optional.empty());

        creationThreadCaptor.getValue().run();
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void testWaitForInstanceNotification_update() throws InterruptedException {
        when(databaseNotificationService.subscribe("terraria_instance_creations")).thenReturn(null);
        when(databaseNotificationService.subscribe("terraria_instance_updates")).thenReturn(new FakeSubscription<>(8L));

        terrariaInstanceDbNotificationService.setUp();
        verify(lifecycleThreadPool, only()).run(creationThreadCaptor.capture(), updateThreadCaptor.capture());
        final TerrariaInstanceEntity instance = mock(TerrariaInstanceEntity.class);
        when(terrariaInstanceRepository.findById(8L)).thenReturn(Optional.of(instance));

        updateThreadCaptor.getValue().run();
        verify(applicationEventPublisher, only()).publishEvent(instanceUpdatedEventCaptor.capture());
        assertSame(instance, instanceUpdatedEventCaptor.getValue().getChangedInstance());
    }

    @Test
    void testInstanceCreated() {
        terrariaInstanceDbNotificationService.instanceCreated(makeInstanceWithId(1L));
        verify(databaseNotificationService, only()).trySend("terraria_instance_creations", 1L);
    }

    @Test
    void testInstanceUpdated() {
        terrariaInstanceDbNotificationService.instanceUpdated(makeInstanceWithId(1L));
        verify(databaseNotificationService, only()).trySend("terraria_instance_updates", 1L);
    }
}