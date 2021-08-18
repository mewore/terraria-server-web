package io.github.mewore.tsw.events;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A publisher that sends values to subscriptions in a Publisher-Subscriber paradigm.
 *
 * @param <T> The data type.
 */
public interface Publisher<@NonNull T, V> {

    /**
     * Publish a value to a specific topic, causing all generic subscriptions and all subscriptions for the specified
     * topic to receive the value.
     *
     * @param topic The topic to publish the value to.
     * @param value The value to publish.
     */
    void publish(final @NonNull T topic, final V value);

    /**
     * Subscribe to all topics.
     *
     * @return The resulting subscription.
     */
    Subscription<V> subscribe();

    /**
     * Subscribe to a specific topic.
     *
     * @param topic The topic to subscribe to.
     * @return The resulting subscription.
     */
    Subscription<V> subscribe(final @NonNull T topic);
}
