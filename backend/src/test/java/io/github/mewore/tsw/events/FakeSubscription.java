package io.github.mewore.tsw.events;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.util.function.Predicate;

import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.Getter;

public class FakeSubscription<T> extends SubscriptionBase<T> {

    private final Queue<T> answers;

    @SafeVarargs
    public FakeSubscription(final T... answers) {
        this.answers = new ArrayDeque<>(Arrays.asList(answers));
    }

    @Getter
    private Duration lastTimeout;

    @Override
    public T take() {
        if (answers.isEmpty()) {
            throw new IllegalArgumentException("Cannot take a non-existent answer");
        }
        return answers.poll();
    }

    @Override
    public @Nullable T waitFor(final Predicate<T> predicate, final Duration timeout) {
        lastTimeout = timeout;
        while (!answers.isEmpty()) {
            final T value = answers.poll();
            if (predicate.test(value)) {
                return value;
            }
        }
        return null;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isOpen() {
        return true;
    }
}
