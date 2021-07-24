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

import static org.junit.jupiter.api.Assertions.assertSame;

@SpringBootTest
class TerrariaInstanceEventServiceIT {

    private static final long INSTANCE_ID = 1;

    @Autowired
    private TerrariaInstanceEventService terrariaInstanceEventService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Test
    void test() throws InterruptedException {
        try (final Subscription<TerrariaInstanceEntity> subscription = terrariaInstanceEventService.subscribe(
                TerrariaInstanceFactory.makeInstanceWithId(INSTANCE_ID))) {
            final TerrariaInstanceEntity instance = TerrariaInstanceFactory.makeInstanceWithId(INSTANCE_ID);
            eventPublisher.publishEvent(new TerrariaInstanceUpdatedEvent(instance));
            assertSame(instance, subscription.waitFor(unusedInstance -> true, Duration.ZERO));
        }
    }
}