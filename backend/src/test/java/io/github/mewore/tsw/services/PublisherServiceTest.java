package io.github.mewore.tsw.services;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mewore.tsw.events.Publisher;
import io.github.mewore.tsw.events.Subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class PublisherServiceTest {

    @InjectMocks
    private PublisherService publisherService;

    @Test
    void testMakePublisher() throws InterruptedException {
        final Publisher<Long, String> publisher = publisherService.makePublisher(null);
        try (final Subscription<String> subscription = publisher.subscribe(1L)) {
            publisher.publish(1L, "value");
            assertEquals("value", subscription.waitFor(value -> true, Duration.ZERO));
        }
    }
}