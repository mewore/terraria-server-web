package io.github.mewore.tsw.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class PublisherTopicEvent<T> {

    private final Type type;

    private final T topic;

    public enum Type {
        /**
         * A topic is considered created when a subscription is created for it while no other subscriptions exist.
         */
        TOPIC_CREATED,

        /**
         * A topic is considered created when a subscription for it is deleted and none other remain.
         */
        TOPIC_DELETED
    }
}
