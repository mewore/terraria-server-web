package io.github.mewore.tsw.events;

import java.time.Duration;
import java.util.function.Predicate;

import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FakeSubscription<T> implements Subscription<T> {

    private final @Nullable T answer;

    @Getter
    private Duration lastTimeout;

    @Override
    public T take() {
        if (answer == null) {
            throw new IllegalArgumentException("Cannot take a null answer");
        }
        return answer;
    }

    @Override
    public @Nullable T waitFor(final Predicate<T> predicate, final Duration timeout) {
        lastTimeout = timeout;
        return answer != null && predicate.test(answer) ? answer : null;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isOpen() {
        return true;
    }
}
