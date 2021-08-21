package io.github.mewore.tsw.services.database;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.impossibl.postgres.api.jdbc.PGConnection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.Synchronized;

@RequiredArgsConstructor
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class ConsistentPostgresConnection implements ConsistentConnection<PGConnection> {

    private final Logger logger = LogManager.getLogger(getClass());

    private final DataSource dataSource;

    private final AtomicReference<@Nullable Consumer<PGConnection>> onConnectReference = new AtomicReference<>(null);

    private volatile @Nullable PGConnection connection = null;

    @Override
    public void onConnect(final Consumer<PGConnection> connectHandler) {
        if (!onConnectReference.compareAndSet(null, connectHandler)) {
            throw new IllegalStateException("Cannot set the on-connect handler twice");
        }
    }

    /**
     * Establish a PostgreSQL connection or reuse an existing one.
     *
     * @return The new connection.
     * @throws SQLException          If establishing the connection fails.
     * @throws IllegalStateException If connected to a database that is not PostgreSQL.
     */
    @Synchronized
    @Override
    public @NonNull PGConnection getOrConnect() throws SQLException {
        final @Nullable PGConnection currentConnection = connection;
        if (currentConnection != null && !currentConnection.isClosed()) {
            return currentConnection;
        }

        final Connection newConnection = dataSource.getConnection();
        if (!newConnection.isWrapperFor(PGConnection.class)) {
            newConnection.close();
            throw new IllegalStateException("The data source does not specify a PostgreSQL connection");
        }

        final PGConnection newPostgresConnection = newConnection.unwrap(PGConnection.class);
        connection = newPostgresConnection;

        final @Nullable Consumer<PGConnection> connectHandler = onConnectReference.get();
        if (connectHandler != null) {
            connectHandler.accept(newPostgresConnection);
        }
        return newPostgresConnection;
    }

    @PreDestroy
    void preDestroy() {
        try {
            final Connection currentConnection = connection;
            if (currentConnection != null && !currentConnection.isClosed()) {
                currentConnection.close();
                connection = null;
            }
        } catch (final SQLException e) {
            logger.error("Failed to disconnect from the PostgreSQL database", e);
            connection = null;
        }
    }
}
