package io.github.mewore.tsw.services.database;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import com.impossibl.postgres.api.jdbc.PGConnection;
import com.impossibl.postgres.api.jdbc.PGNotificationListener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mewore.tsw.events.FakeSubscription;
import io.github.mewore.tsw.events.Publisher;
import io.github.mewore.tsw.events.PublisherTopicEvent;
import io.github.mewore.tsw.events.Subscription;
import io.github.mewore.tsw.services.util.async.InterruptableRunnable;
import io.github.mewore.tsw.services.util.async.LifecycleThreadPool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostgresNotificationServiceTest {

    private static final String IMPOSSIBLE_PREFIX = UUID.randomUUID().toString().replaceAll("[^\\-]", "x") + ":";

    @InjectMocks
    private PostgresNotificationService postgresNotificationService;

    @Mock
    private Publisher<String, String> publisher;

    @Mock
    private ConsistentPostgresConnection postgresConnection;

    @Mock
    private LifecycleThreadPool lifecycleThreadPool;

    @Mock
    private Subscription<PublisherTopicEvent<String>> topicEventSubscription;

    @Mock
    private Subscription<String> subscription;

    @Captor
    private ArgumentCaptor<Consumer<PGConnection>> onConnectCaptor;

    @Captor
    private ArgumentCaptor<PGNotificationListener> pgNotificationListenerCaptor;

    @Captor
    private ArgumentCaptor<InterruptableRunnable> waitForTopicEventsCaptor;

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    @Test
    void testOnConnectCallback() {
        getNotificationListener().notification(1, "channel", IMPOSSIBLE_PREFIX + "payload");
        verify(publisher).publish("channel", "payload");
    }

    @Test
    void testOnConnectCallback_sentPayload() throws SQLException {
        final PGNotificationListener notificationListener = getNotificationListener();

        final Statement statement = mockJdbcStatement();
        postgresNotificationService.send("channel", "payload");
        verify(statement).execute(stringCaptor.capture());
        final String notification = stringCaptor.getValue()
                .replaceFirst("^NOTIFY channel, '", "")
                .replaceFirst("'$", "");

        notificationListener.notification(1, "channel", notification);
        verify(publisher, never()).publish(anyString(), anyString());
    }

    @Test
    void testOnConnectCallback_tooShort() {
        getNotificationListener().notification(1, "channel", "a");
        verify(publisher, never()).publish(anyString(), anyString());
    }

    @Test
    void testOnConnectCallback_closed() {
        getNotificationListener().closed();
    }

    private PGNotificationListener getNotificationListener() {
        postgresNotificationService.setUp();
        verify(postgresConnection, only()).onConnect(onConnectCaptor.capture());

        final PGConnection connection = mock(PGConnection.class);
        onConnectCaptor.getValue().accept(connection);

        verify(connection, only()).addNotificationListener(pgNotificationListenerCaptor.capture());
        return pgNotificationListenerCaptor.getValue();
    }

    @Test
    void testWaitForTopicEvents_closedSubscription() {
        when(publisher.subscribeToTopicEvents()).thenReturn(topicEventSubscription);
        postgresNotificationService.setUp();

        verify(lifecycleThreadPool, only()).run(waitForTopicEventsCaptor.capture());

        when(topicEventSubscription.isOpen()).thenReturn(false);
        final Exception exception = assertThrows(InterruptedException.class,
                () -> waitForTopicEventsCaptor.getValue().run());
        assertEquals("The topic event subscription is closed", exception.getMessage());
    }

    @Test
    void testWaitForTopicEvents_topicCreated() throws InterruptedException, SQLException {
        when(publisher.subscribeToTopicEvents()).thenReturn(
                new FakeSubscription<>(new PublisherTopicEvent<>(PublisherTopicEvent.Type.TOPIC_CREATED, "channel")));
        postgresNotificationService.setUp();

        verify(lifecycleThreadPool, only()).run(waitForTopicEventsCaptor.capture());

        final Statement statement = mockJdbcStatement();
        waitForTopicEventsCaptor.getValue().run();
        verify(statement).execute("LISTEN channel");
        verify(statement).close();
    }

    @Test
    void testWaitForTopicEvents_topicCreated_error() throws InterruptedException, SQLException {
        when(publisher.subscribeToTopicEvents()).thenReturn(
                new FakeSubscription<>(new PublisherTopicEvent<>(PublisherTopicEvent.Type.TOPIC_CREATED, "channel")));
        postgresNotificationService.setUp();

        verify(lifecycleThreadPool, only()).run(waitForTopicEventsCaptor.capture());

        final Statement statement = mockJdbcStatement();
        when(statement.execute("LISTEN channel")).thenThrow(new SQLException("oof"));
        waitForTopicEventsCaptor.getValue().run();
    }

    @Test
    void testWaitForTopicEvents_topicDeleted() throws InterruptedException, SQLException {
        when(publisher.subscribeToTopicEvents()).thenReturn(
                new FakeSubscription<>(new PublisherTopicEvent<>(PublisherTopicEvent.Type.TOPIC_DELETED, "channel")));
        postgresNotificationService.setUp();

        verify(lifecycleThreadPool, only()).run(waitForTopicEventsCaptor.capture());

        final Statement statement = mockJdbcStatement();
        waitForTopicEventsCaptor.getValue().run();
        verify(statement).execute("UNLISTEN channel");
        verify(statement).close();
    }

    @Test
    void testWaitForTopicEvents_topicDeleted_error() throws InterruptedException, SQLException {
        when(publisher.subscribeToTopicEvents()).thenReturn(
                new FakeSubscription<>(new PublisherTopicEvent<>(PublisherTopicEvent.Type.TOPIC_DELETED, "channel")));
        postgresNotificationService.setUp();

        verify(lifecycleThreadPool, only()).run(waitForTopicEventsCaptor.capture());

        final Statement statement = mockJdbcStatement();
        when(statement.execute("UNLISTEN channel")).thenThrow(new SQLException("oof"));
        waitForTopicEventsCaptor.getValue().run();
    }

    @Test
    void testSend() throws SQLException {
        final Statement statement = mockJdbcStatement();
        postgresNotificationService.send("channel", "payload");
        verify(statement).execute(matches(Pattern.compile("^NOTIFY channel, '[a-f0-9\\-]{36}:payload'$")));
        verify(statement).close();
    }

    @Test
    void testSubscribe() throws SQLException {
        when(publisher.subscribe("channel")).thenReturn(subscription);
        assertSame(subscription, postgresNotificationService.subscribe("channel"));
    }

    private Statement mockJdbcStatement() throws SQLException {
        final PGConnection pgConnection = mock(PGConnection.class);
        when(postgresConnection.getOrConnect()).thenReturn(pgConnection);

        final Statement statement = mock(Statement.class);
        when(pgConnection.createStatement()).thenReturn(statement);

        return statement;
    }
}