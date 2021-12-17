package io.github.mewore.tsw.events;

import java.time.Duration;
import java.util.function.Predicate;

import org.checkerframework.checker.nullness.qual.Nullable;

public class NullSubscription<T> extends SubscriptionBase<T> {

    @Override
    public boolean canTake() {
        return false;
    }

    @Override
    public T take() {
        throw new UnsupportedOperationException("Cannot take a value from a null subscription");
    }

    @Override
    public @Nullable T waitFor(final Predicate<T> predicate, final Duration timeout) throws InterruptedException {
        return null;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isOpen() {
        return false;
    }
}
