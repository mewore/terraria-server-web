package io.github.mewore.tsw.events;

import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class QueueSubscriptionTest {

    private static final Runnable DO_NOTHING = () -> {
    };

    private final Logger logger = LogManager.getLogger(getClass());

    @Test
    void testWaitFor() throws InterruptedException {
        try (final ManagedSubscription<Integer> subscription = new QueueSubscription<>(DO_NOTHING, logger, null)) {
            subscription.accept(1);
            assertEquals(1, subscription.waitFor(value -> value == 1, Duration.ZERO));
        }
    }

    @Test
    void testWaitFor_mismatch() throws InterruptedException {
        try (final ManagedSubscription<Integer> subscription = new QueueSubscription<>(DO_NOTHING, logger, null)) {
            subscription.accept(0);
            assertNull(subscription.waitFor(value -> value == 1, Duration.ZERO));
        }
    }

    @Test
    void testWaitFor_none() throws InterruptedException {
        try (final ManagedSubscription<Integer> subscription = new QueueSubscription<>(DO_NOTHING, logger, null)) {
            assertNull(subscription.waitFor(value -> value == 1, Duration.ZERO));
        }
    }

    @Test
    void testWaitFor_fallback() throws InterruptedException {
        try (final ManagedSubscription<Integer> subscription = new QueueSubscription<>(DO_NOTHING, logger, () -> 1)) {
            subscription.accept(0);
            assertEquals(1, subscription.waitFor(value -> value == 1, Duration.ZERO));
        }
    }

    @Test
    void testWaitFor_fallback_mismatch() throws InterruptedException {
        try (final ManagedSubscription<Integer> subscription = new QueueSubscription<>(DO_NOTHING, logger, () -> 0)) {
            subscription.accept(0);
            assertNull(subscription.waitFor(value -> value == 1, Duration.ZERO));
        }
    }

    @Test
    void testWaitFor_overfilled() throws InterruptedException {
        try (final ManagedSubscription<Integer> subscription = new QueueSubscription<>(DO_NOTHING, logger, null)) {
            for (int i = 0; i < 10; i++) {
                subscription.accept(0);
            }
            subscription.accept(1);
            assertNull(subscription.waitFor(value -> value == 1, Duration.ZERO));
        }
    }

    @Test
    void testWaitFor_afterClosed() throws InterruptedException {
        try (final ManagedSubscription<Integer> subscription = new QueueSubscription<>(DO_NOTHING, logger, null)) {
            subscription.close();
            subscription.accept(1);
            assertNull(subscription.waitFor(value -> value == 1, Duration.ZERO));
        }
    }

    @Test
    void testClose() {
        final Runnable onClose = mock(Runnable.class);
        final ManagedSubscription<Integer> subscription = new QueueSubscription<>(onClose, logger, null);

        verify(onClose, never()).run();

        subscription.close();
        verify(onClose).run();
    }

    @Test
    void testClose_closed() {
        final Runnable onClose = mock(Runnable.class);
        final ManagedSubscription<Integer> subscription = new QueueSubscription<>(onClose, logger, null);
        subscription.close();
        subscription.close();
        verify(onClose, atMostOnce()).run();
    }
}