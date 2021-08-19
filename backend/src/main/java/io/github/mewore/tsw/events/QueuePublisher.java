package io.github.mewore.tsw.events;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.TARGET_CLASS)
@Component
public class QueuePublisher<T extends @NonNull Object, V extends @NonNull Object> implements Publisher<T, V> {

    private final AtomicLong currentId = new AtomicLong();

    private final ConcurrentMap<T, ConcurrentMap<Long, ManagedSubscription<V>>> subscriptionsByTopic =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<Long, ManagedSubscription<V>> genericSubscriptions = new ConcurrentHashMap<>();

    @Setter
    private volatile @Nullable Function<T, V> topicToValueMapper;

    @Override
    public void publish(final T topic, final V value) {
        notifySubscriptions(subscriptionsByTopic.get(topic), value);
        notifySubscriptions(genericSubscriptions, value);
    }

    @Override
    public Subscription<V> subscribe() {
        final long id = currentId.incrementAndGet();
        final ManagedSubscription<V> subscription = new QueueSubscription<>(() -> genericSubscriptions.remove(id),
                LogManager.getLogger("GenericSubscription"), null);

        genericSubscriptions.put(id, subscription);
        return subscription;
    }

    @Override
    public Subscription<V> subscribe(final T topic) {
        final long id = currentId.incrementAndGet();
        final var currentTopicToValue = topicToValueMapper;
        final ManagedSubscription<V> subscription = new QueueSubscription<>(() -> {
            final @Nullable ConcurrentMap<Long, ManagedSubscription<V>> map = subscriptionsByTopic.get(topic);
            if (map == null) {
                return;
            }
            map.remove(id);
            if (map.isEmpty()) {
                subscriptionsByTopic.remove(topic, new ConcurrentHashMap<Long, ManagedSubscription<V>>());
            }
        }, LogManager.getLogger("Subscription(" + topic + ")"),
                currentTopicToValue == null ? null : () -> currentTopicToValue.apply(topic));

        subscriptionsByTopic.compute(topic, (key, subscriptionMap) -> {
            final ConcurrentMap<Long, ManagedSubscription<V>> result =
                    subscriptionMap == null ? new ConcurrentHashMap<>(1) : subscriptionMap;
            result.put(id, subscription);
            return result;
        });

        return subscription;
    }

    private void notifySubscriptions(final @Nullable ConcurrentMap<Long, ManagedSubscription<V>> subscriptionMap,
            final V value) {
        if (subscriptionMap != null) {
            for (final ManagedSubscription<V> subscription : subscriptionMap.values()) {
                subscription.accept(value);
            }
        }
    }
}
