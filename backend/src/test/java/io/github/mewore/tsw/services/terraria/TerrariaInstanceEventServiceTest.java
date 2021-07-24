package io.github.mewore.tsw.services.terraria;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mewore.tsw.events.FakeSubscription;
import io.github.mewore.tsw.events.Subscription;
import io.github.mewore.tsw.events.TerrariaInstanceUpdatedEvent;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceRepository;

import static io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory.makeInstanceWithId;
import static io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory.makeInstanceWithState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerrariaInstanceEventServiceTest {

    @InjectMocks
    private TerrariaInstanceEventService terrariaInstanceEventService;

    @Mock
    private TerrariaInstanceRepository terrariaInstanceRepository;

    @Test
    void test() throws InterruptedException {
        try (final Subscription<TerrariaInstanceEntity> subscription = subscribe(1)) {
            final TerrariaInstanceEntity instance = makeInstanceWithId(1);
            emitInstance(instance);
            assertSame(instance, subscription.waitFor(unusedInstance -> true, Duration.ZERO));
        }
    }

    @Test
    void test_fallback() throws InterruptedException {
        try (final Subscription<TerrariaInstanceEntity> subscription = subscribe(1)) {
            emitInstance(makeInstanceWithId(1));
            final TerrariaInstanceEntity fetchedInstance = makeInstanceWithId(1);
            when(terrariaInstanceRepository.getOne(1L)).thenReturn(fetchedInstance);
            assertSame(fetchedInstance, subscription.waitFor(instance -> instance == fetchedInstance, Duration.ZERO));
        }
    }

    @Test
    void test_overfilled() throws InterruptedException {
        try (final Subscription<TerrariaInstanceEntity> subscription = subscribe(1)) {
            final TerrariaInstanceEntity fillerInstance = makeInstanceWithId(1);
            for (int i = 0; i < 10; i++) {
                emitInstance(fillerInstance);
            }
            final TerrariaInstanceEntity instance = makeInstanceWithId(1);
            emitInstance(instance);
            assertNull(subscription.waitFor(instanceToCheck -> instanceToCheck == instance, Duration.ZERO));
        }
    }

    @Test
    void test_openAndClosed() throws InterruptedException {
        try (final Subscription<TerrariaInstanceEntity> subscription = subscribe(1)) {
            try (final Subscription<TerrariaInstanceEntity> closedSubscription = subscribe(1)) {
                closedSubscription.close();
                final TerrariaInstanceEntity instance = makeInstanceWithId(1);
                emitInstance(instance);
                assertSame(instance, subscription.waitFor(unusedInstance -> true, Duration.ZERO));
                assertNull(closedSubscription.waitFor(unusedInstance -> true, Duration.ZERO));
            }
        }
    }

    @Test
    void test_closedSubscription() throws InterruptedException {
        try (final Subscription<TerrariaInstanceEntity> closedSubscription = subscribe(1)) {
            closedSubscription.close();
            emitInstance(makeInstanceWithId(1));
            assertNull(closedSubscription.waitFor(unusedInstance -> true, Duration.ZERO));
        }
    }

    @Test
    void test_unrelatedSubscription() throws InterruptedException {
        try (final Subscription<TerrariaInstanceEntity> unrelatedSubscription = subscribe(2)) {
            emitInstance(makeInstanceWithId(1));
            assertNull(unrelatedSubscription.waitFor(unusedInstance -> true, Duration.ZERO));
        }
    }

    @Test
    void test_manySubscriptions() throws InterruptedException {
        try (final Subscription<TerrariaInstanceEntity> subscription = subscribe(1)) {
            try (final Subscription<TerrariaInstanceEntity> secondSubscription = subscribe(1)) {
                try (final Subscription<TerrariaInstanceEntity> unrelatedSubscription = subscribe(2)) {
                    final TerrariaInstanceEntity instance = makeInstanceWithId(1);
                    emitInstance(instance);

                    final Subscription<TerrariaInstanceEntity> closedSubscription = subscribe(1);
                    closedSubscription.close();

                    assertSame(instance, subscription.waitFor(unusedInstance -> true, Duration.ZERO));
                    assertSame(instance, secondSubscription.waitFor(unusedInstance -> true, Duration.ZERO));
                    assertNull(unrelatedSubscription.waitFor(unusedInstance -> true, Duration.ZERO));
                    assertNull(closedSubscription.waitFor(unusedInstance -> true, Duration.ZERO));
                }
            }
        }
    }

    @Test
    void testWaitForInstanceState() throws InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.IDLE);
        final FakeSubscription<TerrariaInstanceEntity> subscription = new FakeSubscription<>(instance);

        terrariaInstanceEventService.waitForInstanceState(instance, subscription, TerrariaInstanceState.IDLE,
                Duration.ofMinutes(1));
        assertEquals(Duration.ofMinutes(1), subscription.getLastTimeout());
    }

    @Test
    void testWaitForInstanceState_waitForStateFailed() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.IDLE);
        final FakeSubscription<TerrariaInstanceEntity> subscription = new FakeSubscription<>(instance);

        final Exception exception = assertThrows(IllegalStateException.class,
                () -> terrariaInstanceEventService.waitForInstanceState(instance, subscription,
                        TerrariaInstanceState.WORLD_MENU, Duration.ofMinutes(1)));
        assertEquals("The instance " + TerrariaInstanceFactory.INSTANCE_UUID + " did not reach the state WORLD_MENU " +
                "within a timeout of PT1M; instead, its state is IDLE.", exception.getMessage());
    }

    @SuppressWarnings("deprecation")
    private void emitInstance(final TerrariaInstanceEntity instance) {
        terrariaInstanceEventService.onApplicationEvent(new TerrariaInstanceUpdatedEvent(instance));
    }

    private Subscription<TerrariaInstanceEntity> subscribe(final long id) {
        return terrariaInstanceEventService.subscribe(makeInstanceWithId(id));
    }
}