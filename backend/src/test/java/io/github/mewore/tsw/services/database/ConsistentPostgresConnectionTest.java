package io.github.mewore.tsw.services.database;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.function.Consumer;

import com.impossibl.postgres.api.jdbc.PGConnection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsistentPostgresConnectionTest {

    @InjectMocks
    private ConsistentPostgresConnection consistentPostgresConnection;

    @Mock
    private DataSource dataSource;

    @Mock
    private PGConnection connection;

    @Mock
    private Consumer<PGConnection> connectHandler;

    @Test
    void testOnConnect() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isWrapperFor(PGConnection.class)).thenReturn(true);

        final PGConnection unwrappedConnection = mock(PGConnection.class);
        when(connection.unwrap(PGConnection.class)).thenReturn(unwrappedConnection);

        consistentPostgresConnection.onConnect(connectHandler);
        consistentPostgresConnection.getOrConnect();

        verify(connectHandler, only()).accept(unwrappedConnection);
    }

    @Test
    void testGetOrConnect() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isWrapperFor(PGConnection.class)).thenReturn(true);

        final PGConnection unwrappedConnection = mock(PGConnection.class);
        when(connection.unwrap(PGConnection.class)).thenReturn(unwrappedConnection);

        assertSame(unwrappedConnection, consistentPostgresConnection.getOrConnect());
    }

    @Test
    void testGetOrConnect_notPostgres() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isWrapperFor(PGConnection.class)).thenReturn(false);

        final Exception exception = assertThrows(IllegalStateException.class,
                () -> consistentPostgresConnection.getOrConnect());

        assertEquals("The data source does not specify a PostgreSQL connection", exception.getMessage());
    }

    @Test
    void testGetOrConnect_twice() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isWrapperFor(PGConnection.class)).thenReturn(true);
        when(connection.unwrap(PGConnection.class)).thenReturn(connection);

        assertSame(connection, consistentPostgresConnection.getOrConnect());

        when(connection.isClosed()).thenReturn(false);
        assertSame(connection, consistentPostgresConnection.getOrConnect());
    }

    @Test
    void testGetOrConnect_twice_closed() throws SQLException {
        final PGConnection secondConnection = mock(PGConnection.class);
        when(secondConnection.isWrapperFor(PGConnection.class)).thenReturn(true);
        when(secondConnection.unwrap(PGConnection.class)).thenReturn(secondConnection);

        when(dataSource.getConnection()).thenReturn(connection).thenReturn(secondConnection);
        when(connection.isWrapperFor(PGConnection.class)).thenReturn(true);
        when(connection.unwrap(PGConnection.class)).thenReturn(connection);
        assertSame(connection, consistentPostgresConnection.getOrConnect());

        when(connection.isClosed()).thenReturn(true);
        assertSame(secondConnection, consistentPostgresConnection.getOrConnect());
    }

    @Test
    void testPreDestroy() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isWrapperFor(PGConnection.class)).thenReturn(true);

        final PGConnection unwrappedConnection = mock(PGConnection.class);
        when(connection.unwrap(PGConnection.class)).thenReturn(unwrappedConnection);
        consistentPostgresConnection.getOrConnect();

        when(unwrappedConnection.isClosed()).thenReturn(false);
        consistentPostgresConnection.preDestroy();

        verify(unwrappedConnection).close();
    }

    @Test
    void testPreDestroy_alreadyClosed() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isWrapperFor(PGConnection.class)).thenReturn(true);
        when(connection.unwrap(PGConnection.class)).thenReturn(connection);
        consistentPostgresConnection.getOrConnect();

        when(connection.isClosed()).thenReturn(true);
        consistentPostgresConnection.preDestroy();

        verify(connection, never()).close();
    }

    @Test
    void testPreDestroy_noConnection() {
        consistentPostgresConnection.preDestroy();
    }

    @Test
    void testPreDestroy_error() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isWrapperFor(PGConnection.class)).thenReturn(true);
        when(connection.unwrap(PGConnection.class)).thenReturn(connection);
        consistentPostgresConnection.getOrConnect();

        when(connection.isClosed()).thenReturn(false);
        doThrow(new SQLException("oof")).when(connection).close();
        consistentPostgresConnection.preDestroy();
    }
}