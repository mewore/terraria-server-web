package io.github.mewore.tsw.services.database;

import com.fasterxml.jackson.core.type.TypeReference;

import org.springframework.stereotype.Service;

import io.github.mewore.tsw.events.NullSubscription;
import io.github.mewore.tsw.events.Subscription;

@Service
class NullDatabaseNotificationService implements DatabaseNotificationService {

    @Override
    public void sendRaw(final String channel, final String content) {
    }

    @Override
    public <T> void send(final String channel, final T content) {
    }

    @Override
    public <T> void trySend(final String channel, final T content) {
    }

    @Override
    public Subscription<String> subscribeRaw(final String channel) {
        return new NullSubscription<>();
    }

    @Override
    public <T> Subscription<T> subscribe(final String channel, final TypeReference<T> typeReference) {
        return new NullSubscription<>();
    }
}
