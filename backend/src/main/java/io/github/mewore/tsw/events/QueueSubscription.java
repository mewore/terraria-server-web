package io.github.mewore.tsw.events;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class QueueSubscription<T extends @NonNull Object> implements ManagedSubscription<T> {

    private static final int QUEUE_CAPACITY = 10;

    @Getter
    private final BlockingQueue<T> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

    private final Runnable onClosed;

    private final Logger logger;

    /**
     * Used as a last-ditch attempt if waiting for the value fails.
     */
    private final @Nullable Supplier<T> valueSupplier;

    private final AtomicBoolean opened = new AtomicBoolean(true);

    @Override
    public void accept(final T value) {
        if (opened.get() && !queue.offer(value)) {
            logger.warn("The subscription blocking queue for has been overfilled! Skipping the next value.");
        }
    }

    @Override
    public T take() throws InterruptedException {
        return queue.take();
    }

    @Override
    public @Nullable T waitFor(final Predicate<T> predicate, final Duration timeout) throws InterruptedException {
        final Instant deadline = Instant.now().plus(timeout);
        while (true) {
            final @Nullable T result = queue.poll(Instant.now().until(deadline, ChronoUnit.MILLIS),
                    TimeUnit.MILLISECONDS);
            if (result == null) {
                break;
            }
            if (predicate.test(result)) {
                return result;
            }
        }
        if (valueSupplier == null) {
            return null;
        }
        final T result = valueSupplier.get();
        return predicate.test(result) ? result : null;
    }

    @Override
    public void close() {
        if (opened.compareAndSet(true, false)) {
            logger.debug("[Closing]");
            onClosed.run();
        }
    }

    @Override
    public boolean isOpen() {
        return opened.get();
    }
}
