package io.github.mewore.tsw.events;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubscriptionBaseTest {

    @Test
    void testTake() throws InterruptedException {
        assertEquals(2, new FakeSubscription<>(1, 2, 3).take(value -> value == 2));
    }

    @Test
    void testMap() throws InterruptedException {
        assertEquals(1, new FakeSubscription<>("1").map(Integer::valueOf).take());
    }
}