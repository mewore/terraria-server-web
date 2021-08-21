package io.github.mewore.tsw.events;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NullSubscriptionTest {

    @Test
    void testTake() {
        final Exception exception = assertThrows(UnsupportedOperationException.class,
                () -> new NullSubscription<>().take());
        assertEquals("Cannot take a value from a null subscription", exception.getMessage());
    }

    @Test
    void testWaitFor() throws InterruptedException {
        assertNull(new NullSubscription<>().waitFor(unused -> true, Duration.ZERO));
    }

    @Test
    void testIsOpen() {
        assertFalse(new NullSubscription<>().isOpen());
    }
}
