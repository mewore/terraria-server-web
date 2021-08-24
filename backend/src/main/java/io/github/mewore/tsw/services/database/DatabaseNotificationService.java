package io.github.mewore.tsw.services.database;

import java.sql.SQLException;

import com.fasterxml.jackson.core.JsonProcessingException;

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
    void sendRaw(final String channel, final String content) throws SQLException;

    /**
     * Send a notification to the database. Automatically serialize the payload into JSON.
     *
     * @param channel The channel to send the notification to.
     * @param content The content of the notification.
     * @param <T>     The content type.
     * @throws SQLException            If the connection to the database or the execution of the SQL statement for the
     *                                 notification fails.
     * @throws JsonProcessingException If the serialization of the content fails.
     */
    <T> void send(final String channel, final T content) throws SQLException, JsonProcessingException;

    /**
     * Send a notification to the database. Automatically serialize the payload into JSON. Instead of propagating
     * checked exceptions, the exceptions are logged.
     *
     * @param channel The channel to send the notification to.
     * @param content The content of the notification.
     * @param <T>     The content type.
     */
    <T> void trySend(final String channel, final T content);

    /**
     * Listen for notifications coming from the database.
     *
     * @param channel The channel to listen for notifications from.
     * @return The subscription for the channel.
     */
    Subscription<String> subscribeRaw(final String channel);

    /**
     * Listen for notifications coming from the database. Automatically deserialize the content from JSON.
     *
     * @param <T>     The content type.
     * @param channel The channel to listen for notifications from.
     * @return The subscription for the channel.
     */
    <T> Subscription<T> subscribe(final String channel);
}
