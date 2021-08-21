package io.github.mewore.tsw.events;

import java.time.Duration;
import java.util.function.Predicate;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A subscription that receives values in a Publisher-Subscriber paradigm.
 *
 * @param <T> The data type.
 */
public interface Subscription<T> extends AutoCloseable {

    /**
     * Take the first received value for this subscription that hasn't been acknowledged yet.
     *
     * @return The first value that is received.
     * @throws InterruptedException If interrupted while waiting.
     */
    T take() throws InterruptedException;

    /**
     * Wait until a value matching a predicate is received.
     *
     * @param predicate The predicate to filter the values by.
     * @param timeout   The time to wait for such a value for.
     * @return The first value that matches the predicate, or {@code null} if the time has run out before any such
     * value has been encountered.
     * @throws InterruptedException If interrupted while waiting.
     */
    @Nullable T waitFor(final Predicate<T> predicate, final Duration timeout) throws InterruptedException;

    /**
     * An exception-free version of {@link AutoCloseable#close()}. Subscriptions of this kind are not allowed to
     * throw any exceptions when closed.
     */
    @Override
    void close();

    /**
     * @return @Whether the subscription is still open.
     */
    boolean isOpen();
}
