package io.github.mewore.tsw.events;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Predicate;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class MappedSubscription<FROM extends @NonNull Object, T extends @NonNull Object> extends SubscriptionBase<T> {

    private final Subscription<FROM> originalSubscription;

    private final Function<FROM, T> valueMapper;

    @Override
    public T take() throws InterruptedException {
        return valueMapper.apply(originalSubscription.take());
    }

    @Override
    public @Nullable T waitFor(final Predicate<T> predicate, final Duration timeout) throws InterruptedException {
        final @Nullable FROM result = originalSubscription.waitFor(predicateForOriginalValues(predicate), timeout);
        return result == null ? null : valueMapper.apply(result);
    }

    @Override
    public void close() {
        originalSubscription.close();
    }

    @Override
    public boolean isOpen() {
        return originalSubscription.isOpen();
    }

    private Predicate<FROM> predicateForOriginalValues(final Predicate<T> predicate) {
        return candidate -> predicate.test(valueMapper.apply(candidate));
    }
}
