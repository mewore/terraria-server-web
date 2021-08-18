package io.github.mewore.tsw.services.terraria;

import java.time.Duration;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mewore.tsw.events.FakeSubscription;
import io.github.mewore.tsw.events.Publisher;
import io.github.mewore.tsw.events.Subscription;
import io.github.mewore.tsw.events.TerrariaInstanceUpdatedEvent;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceRepository;
import io.github.mewore.tsw.services.PublisherService;

import static io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory.makeInstanceWithId;
import static io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory.makeInstanceWithState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerrariaInstanceSubscriptionServiceTest {

    private TerrariaInstanceSubscriptionService terrariaInstanceSubscriptionService;

    @Mock
    private TerrariaInstanceRepository terrariaInstanceRepository;

    @Mock
    private PublisherService publisherService;

    @Mock
    private Publisher<Long, TerrariaInstanceEntity> publisher;

    @Captor
    private ArgumentCaptor<Function<Long, TerrariaInstanceEntity>> valueSupplierCaptor;

    @BeforeEach
    void setUp() {
        when(publisherService.<Long, TerrariaInstanceEntity>makePublisher(any())).thenReturn(publisher);
        terrariaInstanceSubscriptionService = new TerrariaInstanceSubscriptionService(terrariaInstanceRepository,
                publisherService);
    }

    @Test
    void testFallback() {
        verify(publisherService, only()).makePublisher(valueSupplierCaptor.capture());
        final TerrariaInstanceEntity fetchedInstance = mock(TerrariaInstanceEntity.class);
        when(terrariaInstanceRepository.getOne(1L)).thenReturn(fetchedInstance);

        final TerrariaInstanceEntity result = valueSupplierCaptor.getValue().apply(1L);
        assertSame(fetchedInstance, result);
    }

    @SuppressWarnings("deprecation")
    @Test
    void testOnApplicationEvent() {
        final TerrariaInstanceEntity instance = makeInstanceWithId(1);
        terrariaInstanceSubscriptionService.onApplicationEvent(new TerrariaInstanceUpdatedEvent(instance));
        verify(publisher).publish(eq(1L), same(instance));
    }

    @Test
    void testSubscribe() {
        final Subscription<TerrariaInstanceEntity> subscription = new FakeSubscription<>(null);
        when(publisher.subscribe(1L)).thenReturn(subscription);

        final Subscription<TerrariaInstanceEntity> result = terrariaInstanceSubscriptionService.subscribe(
                makeInstanceWithId(1));
        assertSame(subscription, result);
    }

    @Test
    void testSubscribeToAll() {
        final Subscription<TerrariaInstanceEntity> subscription = new FakeSubscription<>(null);
        when(publisher.subscribe()).thenReturn(subscription);

        final Subscription<TerrariaInstanceEntity> result = terrariaInstanceSubscriptionService.subscribeToAll();
        assertSame(subscription, result);
    }

    @Test
    void testWaitForInstanceState() throws InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.IDLE);
        final FakeSubscription<TerrariaInstanceEntity> subscription = new FakeSubscription<>(instance);

        terrariaInstanceSubscriptionService.waitForInstanceState(instance, subscription, Duration.ofMinutes(1),
                TerrariaInstanceState.IDLE);
        assertEquals(Duration.ofMinutes(1), subscription.getLastTimeout());
    }

    @Test
    void testWaitForInstanceState_waitForStateFailed() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.IDLE);
        final FakeSubscription<TerrariaInstanceEntity> subscription = new FakeSubscription<>(instance);

        final Exception exception = assertThrows(IllegalStateException.class,
                () -> terrariaInstanceSubscriptionService.waitForInstanceState(instance, subscription,
                        Duration.ofMinutes(1), TerrariaInstanceState.WORLD_MENU, TerrariaInstanceState.BOOTING_UP));
        assertEquals("The instance " + TerrariaInstanceFactory.INSTANCE_UUID +
                " did not reach the state(s) WORLD_MENU/BOOTING_UP " +
                "within a timeout of PT1M; instead, its state is IDLE.", exception.getMessage());
    }
}