package io.github.mewore.tsw.events;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class QueuePublisherTest {

    @Test
    void testSubscribe_specific() throws InterruptedException {
        final Publisher<Integer, Integer> publisher = new QueuePublisher<>();
        try (final Subscription<Integer> subscription = publisher.subscribe(1)) {
            publisher.publish(1, 1);
            assertEquals(1, subscription.waitFor(value -> true, Duration.ZERO));
        }
    }

    @Test
    void testSubscribe_fallback() throws InterruptedException {
        final Publisher<Integer, Integer> publisher = new QueuePublisher<>();
        publisher.setTopicToValueMapper(topic -> topic * 5);
        try (final Subscription<Integer> subscription = publisher.subscribe(1)) {
            publisher.publish(1, 1);
            assertEquals(5, subscription.waitFor(value -> value == 5, Duration.ZERO));
        }
    }

    @Test
    void testSubscribe_unrelatedSubscription() throws InterruptedException {
        final Publisher<Integer, Integer> publisher = new QueuePublisher<>();
        try (final Subscription<Integer> subscription = publisher.subscribe(2)) {
            publisher.publish(1, 1);
            assertNull(subscription.waitFor(value -> true, Duration.ZERO));
        }
    }

    @Test
    void testSubscribe_twoSubscriptions() throws InterruptedException {
        final Publisher<Integer, Integer> publisher = new QueuePublisher<>();
        try (final Subscription<Integer> subscription = publisher.subscribe(1)) {
            final Subscription<Integer> otherSubscription = publisher.subscribe(1);

            publisher.publish(1, 1);
            assertEquals(1, otherSubscription.waitFor(value -> true, Duration.ZERO));
            assertEquals(1, subscription.waitFor(value -> true, Duration.ZERO));

            otherSubscription.close();
            publisher.publish(1, 2);
            assertNull(otherSubscription.waitFor(value -> true, Duration.ZERO));
            assertEquals(2, subscription.waitFor(value -> true, Duration.ZERO));
        }
    }

    @Test
    void testSubscribe_generic() throws InterruptedException {
        final Publisher<Integer, Integer> publisher = new QueuePublisher<>();
        try (final Subscription<Integer> subscription = publisher.subscribe()) {
            publisher.publish(1, 1);
            assertEquals(1, subscription.waitFor(value -> true, Duration.ZERO));

            publisher.publish(2, 2);
            assertEquals(2, subscription.waitFor(value -> true, Duration.ZERO));
        }
    }
}