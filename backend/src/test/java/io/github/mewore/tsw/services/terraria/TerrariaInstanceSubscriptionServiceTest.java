package io.github.mewore.tsw.services.terraria;

import java.time.Duration;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mewore.tsw.events.FakeSubscription;
import io.github.mewore.tsw.events.Publisher;
import io.github.mewore.tsw.events.Subscription;
import io.github.mewore.tsw.events.TerrariaInstanceApplicationEvent;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceRepository;

import static io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory.makeInstanceWithId;
import static io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory.makeInstanceWithState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerrariaInstanceSubscriptionServiceTest {

    @InjectMocks
    private TerrariaInstanceSubscriptionService terrariaInstanceSubscriptionService;

    @Mock
    private TerrariaInstanceRepository terrariaInstanceRepository;

    @Mock
    private Publisher<Long, TerrariaInstanceEntity> publisher;

    @Mock
    private TerrariaMessageService terrariaMessageService;

    @Captor
    private ArgumentCaptor<Function<Long, TerrariaInstanceEntity>> valueSupplierCaptor;

    @Test
    void testSetUp() {
        terrariaInstanceSubscriptionService.setUp();
        verify(publisher, only()).setTopicToValueMapper(valueSupplierCaptor.capture());

        final TerrariaInstanceEntity fetchedInstance = mock(TerrariaInstanceEntity.class);
        when(terrariaInstanceRepository.getOne(1L)).thenReturn(fetchedInstance);
        assertSame(fetchedInstance, valueSupplierCaptor.getValue().apply(1L));
    }

    @SuppressWarnings("deprecation")
    @Test
    void testOnApplicationEvent_created() {
        final TerrariaInstanceEntity instance = makeInstanceWithId(1);
        terrariaInstanceSubscriptionService.onApplicationEvent(new TerrariaInstanceApplicationEvent(instance, true));
        verify(publisher).publish(eq(1L), same(instance));
        verify(terrariaMessageService, only()).broadcastInstanceCreation(same(instance));
    }

    @SuppressWarnings("deprecation")
    @Test
    void testOnApplicationEvent_changed() {
        final TerrariaInstanceEntity instance = makeInstanceWithId(1);
        terrariaInstanceSubscriptionService.onApplicationEvent(new TerrariaInstanceApplicationEvent(instance, false));
        verify(publisher).publish(eq(1L), same(instance));
        verify(terrariaMessageService, only()).broadcastInstanceChange(same(instance));
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