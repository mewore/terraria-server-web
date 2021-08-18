package io.github.mewore.tsw.services;

import java.util.function.Function;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.events.Publisher;
import io.github.mewore.tsw.events.QueuePublisher;

@Service
public class PublisherService {

    public <T extends @NonNull Object, V extends @NonNull Object> Publisher<T, V> makePublisher(final @Nullable Function<T, V> valueSupplier) {
        return new QueuePublisher<>(valueSupplier);
    }
}
