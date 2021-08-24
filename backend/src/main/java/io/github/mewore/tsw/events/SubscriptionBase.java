package io.github.mewore.tsw.events;

import java.util.function.Function;
import java.util.function.Predicate;

abstract class SubscriptionBase<T> implements Subscription<T> {

    @Override
    public T take(final Predicate<T> predicate) throws InterruptedException {
        while (true) {
            final T value = take();
            if (predicate.test(value)) {
                return value;
            }
        }
    }

    @Override
    public <TO> Subscription<TO> map(final Function<T, TO> valueMapper) {
        return new MappedSubscription<>(this, valueMapper);
    }
}
