package io.github.mewore.tsw.services.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.NonNull;

public interface ConsistentConnection<T extends Connection> {

    void onConnect(Consumer<T> onConnectHandler);

    @NonNull T getOrConnect() throws SQLException;
}
