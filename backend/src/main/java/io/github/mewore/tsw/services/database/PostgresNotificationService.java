package io.github.mewore.tsw.services.database;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
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

    private static final ObjectWriter JSON_WRITER = new ObjectMapper().writer();

    private static final ObjectReader JSON_READER = new ObjectMapper().configure(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).reader();

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
    public void sendRaw(final String channel, final String content) throws SQLException {
        try (final Statement statement = postgresConnection.getOrConnect().createStatement()) {
            logger.info("Sending notification to " + channel + ": " + PREFIX + content);
            statement.execute("NOTIFY " + channel + ", '" + PREFIX + content + "'");
        }
    }

    @Override
    public <T> void send(final String channel, final T content) throws SQLException, JsonProcessingException {
        sendRaw(channel, JSON_WRITER.writeValueAsString(content));
    }

    @Override
    public <T> void trySend(final String channel, final T content) {
        try {
            send(channel, content);
        } catch (final JsonProcessingException e) {
            logger.error("Failed to serialize " + content + " into JSON", e);
        } catch (final SQLException e) {
            logger.error("Failed to send a notification to channel \"" + channel + "\"", e);
        }
    }

    @Override
    public Subscription<String> subscribeRaw(final String channel) {
        return publisher.subscribe(channel);
    }

    @Override
    public <T> Subscription<T> subscribe(final String channel) {
        final TypeReference<T> typeReference = new TypeReference<>() {
        };
        return subscribeRaw(channel).map(raw -> {
            try {
                return JSON_READER.readValue(JSON_READER.createParser(raw), typeReference);
            } catch (final IOException e) {
                final String errorMessage = "Failed to parse the raw notification <" + raw + "> as JSON";
                logger.error(errorMessage, e);
                throw new RuntimeException(errorMessage, e);
            }
        });
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
