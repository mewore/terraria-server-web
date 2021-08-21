package io.github.mewore.tsw.services.database;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.github.mewore.tsw.events.Subscription;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NullDatabaseNotificationServiceTest {

    @Test
    void send() {
        new NullDatabaseNotificationService().send("channel", "content");
    }

    @Test
    void subscribe_waitFor() throws InterruptedException {
        try (final Subscription<String> subscription = new NullDatabaseNotificationService().subscribe("channel")) {
            assertNull(subscription.waitFor(unused -> true, Duration.ZERO));
        }
    }

    @Test
    void subscribe_take() {
        try (final Subscription<String> subscription = new NullDatabaseNotificationService().subscribe("channel")) {
            assertThrows(UnsupportedOperationException.class, subscription::take);
        }
    }
}