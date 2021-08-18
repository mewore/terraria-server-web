package io.github.mewore.tsw.services.terraria;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;

import io.github.mewore.tsw.events.Subscription;
import io.github.mewore.tsw.events.TerrariaInstanceUpdatedEvent;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory;
import io.github.mewore.tsw.services.PublisherService;

import static org.junit.jupiter.api.Assertions.assertSame;

@SpringBootTest
class TerrariaInstanceSubscriptionServiceIT {

    private static final long INSTANCE_ID = 1;

    @Autowired
    private TerrariaInstanceSubscriptionService terrariaInstanceSubscriptionService;

    @Autowired
    private PublisherService publisherService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Test
    void test() throws InterruptedException {
        try (final Subscription<TerrariaInstanceEntity> subscription = terrariaInstanceSubscriptionService.subscribe(
                TerrariaInstanceFactory.makeInstanceWithId(INSTANCE_ID))) {
            final TerrariaInstanceEntity instance = TerrariaInstanceFactory.makeInstanceWithId(INSTANCE_ID);
            eventPublisher.publishEvent(new TerrariaInstanceUpdatedEvent(instance));
            assertSame(instance, subscription.waitFor(unusedInstance -> true, Duration.ZERO));
        }
    }

    @Test
    void test_generic() throws InterruptedException {
        try (final Subscription<TerrariaInstanceEntity> subscription =
                     terrariaInstanceSubscriptionService.subscribeToAll()) {
            final TerrariaInstanceEntity instance = TerrariaInstanceFactory.makeInstanceWithId(INSTANCE_ID);
            eventPublisher.publishEvent(new TerrariaInstanceUpdatedEvent(instance));
            assertSame(instance, subscription.waitFor(unusedInstance -> true, Duration.ZERO));
        }
    }
}