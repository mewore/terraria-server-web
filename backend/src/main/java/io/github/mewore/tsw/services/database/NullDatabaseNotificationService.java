package io.github.mewore.tsw.services.database;

import org.springframework.stereotype.Service;

import io.github.mewore.tsw.events.NullSubscription;
import io.github.mewore.tsw.events.Subscription;

@Service
class NullDatabaseNotificationService implements DatabaseNotificationService {

    @Override
    public void send(final String channel, final String content) {
    }

    @Override
    public Subscription<String> subscribe(final String channel) {
        return new NullSubscription<>();
    }
}
