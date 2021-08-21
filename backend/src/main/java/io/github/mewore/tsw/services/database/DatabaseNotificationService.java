package io.github.mewore.tsw.services.database;

import java.sql.SQLException;

import io.github.mewore.tsw.events.Subscription;

public interface DatabaseNotificationService {

    /**
     * Send a notification to the database.
     *
     * @param channel The channel to send the notification to.
     * @param content The content of the notification.
     * @throws SQLException If the connection to the database or the execution of the SQL statement for the
     *                      notification fails.
     */
    void send(final String channel, final String content) throws SQLException;

    /**
     * Listen for notifications coming from the database.
     *
     * @param channel The channel to listen for notifications from.
     * @throws SQLException If the connection to the database or the execution of the SQL statement for the
     *                      subscription to the channel fails.
     */
    Subscription<String> subscribe(final String channel) throws SQLException;
}
