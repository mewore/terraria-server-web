package io.github.mewore.tsw.events;

import java.time.Duration;
import java.util.function.Predicate;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface Subscription<T> extends AutoCloseable {

    @Nullable T waitFor(final Predicate<T> predicate, final Duration timeout) throws InterruptedException;

    @Override
    void close();
}
