package io.github.mewore.tsw.events;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MappedSubscriptionTest {

    @Mock
    private Subscription<String> sourceSubscription;

    @Test
    void testTake() throws InterruptedException {
        when(sourceSubscription.take()).thenReturn("10");
        assertEquals(11, makeSubscription().take());
    }

    @Test
    void testWaitFor() throws InterruptedException {
        assertEquals(11, makeSubscription(new FakeSubscription<>("10")).waitFor(value -> value == 11, Duration.ZERO));
    }

    @Test
    void testWaitFor_null() throws InterruptedException {
        when(sourceSubscription.waitFor(any(), any())).thenReturn(null);
        assertNull(makeSubscription().waitFor(value -> true, Duration.ZERO));
    }

    @Test
    void testClose() {
        makeSubscription().close();
        verify(sourceSubscription, only()).close();
    }

    @Test
    void testIsOpen() {
        when(sourceSubscription.isOpen()).thenReturn(true);
        assertTrue(makeSubscription().isOpen());
    }

    private Subscription<Integer> makeSubscription() {
        return makeSubscription(sourceSubscription);
    }

    private Subscription<Integer> makeSubscription(final Subscription<String> subscription) {
        return new MappedSubscription<>(subscription, value -> Integer.parseInt(value) + 1);
    }
}