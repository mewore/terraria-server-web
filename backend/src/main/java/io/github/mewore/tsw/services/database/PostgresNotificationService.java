package io.github.mewore.tsw.services.database;

import javax.annotation.PostConstruct;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import com.impossibl.postgres.api.jdbc.PGNotificationListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.events.Publisher;
import io.github.mewore.tsw.events.PublisherTopicEvent;
import io.github.mewore.tsw.events.Subscription;
import io.github.mewore.tsw.services.util.async.LifecycleThreadPool;
import lombok.RequiredArgsConstructor;

@Profile({"test", "local-postgres", "dev", "prod"})
@Primary
@Service
@RequiredArgsConstructor
class PostgresNotificationService implements DatabaseNotificationService {

    private static final String PREFIX = UUID.randomUUID() + ":";

    private final Logger logger = LogManager.getLogger(getClass());

    private final Publisher<String, String> publisher;

    private final ConsistentPostgresConnection postgresConnection;

    private final LifecycleThreadPool lifecycleThreadPool;

    @PostConstruct
    void setUp() {
        postgresConnection.onConnect(
                newConnection -> newConnection.addNotificationListener(new PostgresNotificationPublisher(publisher)));

        final Subscription<PublisherTopicEvent<String>> topicEventSubscription = publisher.subscribeToTopicEvents();
        lifecycleThreadPool.run(() -> waitForTopicEvents(topicEventSubscription));
    }

    private void waitForTopicEvents(final Subscription<PublisherTopicEvent<String>> topicEventSubscription)
            throws InterruptedException {
        if (!topicEventSubscription.isOpen()) {
            throw new InterruptedException("The topic event subscription is closed");
        }
        final PublisherTopicEvent<String> event = topicEventSubscription.take();
        if (event.getType() == PublisherTopicEvent.Type.TOPIC_CREATED) {
            try {
                listen(event.getTopic());
            } catch (final SQLException e) {
                logger.error("Failed to listen to channel " + event.getTopic());
            }
        } else if (event.getType() == PublisherTopicEvent.Type.TOPIC_DELETED) {
            try {
                unlisten(event.getTopic());
            } catch (final SQLException e) {
                logger.error("Failed to stop listening to channel " + event.getTopic());
            }
        }
    }

    @Override
    public void send(final String channel, final String payload) throws SQLException {
        try (final Statement statement = postgresConnection.getOrConnect().createStatement()) {
            logger.info("Sending notification to " + channel + ": " + PREFIX + payload);
            statement.execute("NOTIFY " + channel + ", '" + PREFIX + payload + "'");
        }
    }

    @Override
    public Subscription<String> subscribe(final String channel) throws SQLException {
        return publisher.subscribe(channel);
    }

    private void listen(final String channel) throws SQLException {
        try (final Statement statement = postgresConnection.getOrConnect().createStatement()) {
            logger.info("Listening to " + channel + "...");
            statement.execute("LISTEN " + channel);
        }
    }

    private void unlisten(final String channel) throws SQLException {
        try (final Statement statement = postgresConnection.getOrConnect().createStatement()) {
            logger.info("Stopping listening to " + channel + "...");
            statement.execute("UNLISTEN " + channel);
        }
    }

    @RequiredArgsConstructor
    private static class PostgresNotificationPublisher implements PGNotificationListener {

        private final Logger logger = LogManager.getLogger(getClass());

        private final Publisher<String, String> publisher;

        @Override
        public void notification(final int processId, final String channelName, final String payload) {
            if (payload.length() <= PREFIX.length()) {
                logger.warn("The notification \"{}\" isn't longer than the prefix \"{}\"", payload, PREFIX);
                return;
            }
            if (payload.startsWith(PREFIX)) {
                logger.debug("Received notification from self: " + payload);
                return;
            }
            logger.info("Received PostgreSQL notification from process {} on channel {}: {}", processId, channelName,
                    payload);
            publisher.publish(channelName, payload.substring(PREFIX.length()));
        }

        @Override
        public void closed() {
            logger.warn("PostgreSQL connection closed");
        }
    }
}
