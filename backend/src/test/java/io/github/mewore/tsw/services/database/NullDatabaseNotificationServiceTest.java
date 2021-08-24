package io.github.mewore.tsw.services.database;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.github.mewore.tsw.events.Subscription;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NullDatabaseNotificationServiceTest {

    @Test
    void testSendRaw() {
        new NullDatabaseNotificationService().sendRaw("channel", "content");
    }

    @Test
    void testTrySend() {
        new NullDatabaseNotificationService().trySend("channel", "content");
    }

    @Test
    void testSubscribeRaw_waitFor() throws InterruptedException {
        try (final Subscription<String> subscription = new NullDatabaseNotificationService().subscribeRaw("channel")) {
            assertNull(subscription.waitFor(unused -> true, Duration.ZERO));
        }
    }

    @Test
    void testSubscribeRaw_take() {
        try (final Subscription<String> subscription = new NullDatabaseNotificationService().subscribeRaw("channel")) {
            assertThrows(UnsupportedOperationException.class, subscription::take);
        }
    }

    @Test
    void testSubscribe() {
        try (final Subscription<String> subscription = new NullDatabaseNotificationService().subscribe("channel")) {
            assertThrows(UnsupportedOperationException.class, subscription::take);
        }
    }
}