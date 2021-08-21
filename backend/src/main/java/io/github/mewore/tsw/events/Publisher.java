package io.github.mewore.tsw.events;

import java.util.function.Function;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A publisher that sends values to subscriptions in a Publisher-Subscriber paradigm.
 *
 * @param <T> The data type.
 */
public interface Publisher<T, V> {

    /**
     * @param newTopicToValueMapper The function used to map a topic to a value that will be used as a fallback if no
     *                              matching values have been encountered by a topic-specific subscription.
     */
    void setTopicToValueMapper(final @Nullable Function<T, V> newTopicToValueMapper);

    /**
     * Publish a value to a specific topic, causing all generic subscriptions and all subscriptions for the specified
     * topic to receive the value.
     *
     * @param topic The topic to publish the value to.
     * @param value The value to publish.
     */
    void publish(final T topic, final V value);

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
    Subscription<V> subscribe(final T topic);

    /**
     * Subscribe to all topic events that occur from now on. A topic is considered created when a subscription is
     * created for it while no other subscriptions exist.
     *
     * @return The subscription for all created topics.
     */
    Subscription<PublisherTopicEvent<T>> subscribeToTopicEvents();
}
