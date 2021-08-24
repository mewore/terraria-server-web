package io.github.mewore.tsw.services.terraria;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;

import io.github.mewore.tsw.config.TestConfig;
import io.github.mewore.tsw.events.Subscription;
import io.github.mewore.tsw.events.TerrariaInstanceApplicationEvent;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceRepository;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import(TestConfig.class)
@SpringBootTest
class TerrariaInstanceSubscriptionServiceIT {

    private static final long INSTANCE_ID = 1;

    @Autowired
    private TerrariaInstanceSubscriptionService terrariaInstanceSubscriptionService;

    @MockBean
    private TerrariaInstanceRepository terrariaInstanceRepository;

    @MockBean
    private TerrariaInstanceMessageService terrariaInstanceMessageService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Test
    void test() throws InterruptedException {
        try (final Subscription<TerrariaInstanceEntity> subscription = terrariaInstanceSubscriptionService.subscribe(
                TerrariaInstanceFactory.makeInstanceWithId(INSTANCE_ID))) {
            final TerrariaInstanceEntity instance = TerrariaInstanceFactory.makeInstanceWithId(INSTANCE_ID);
            eventPublisher.publishEvent(new TerrariaInstanceApplicationEvent(instance, true));
            assertSame(instance, subscription.waitFor(unusedInstance -> true, Duration.ZERO));
            verify(terrariaInstanceRepository, never()).getOne(anyLong());
            verify(terrariaInstanceMessageService, only()).broadcastInstanceCreation(instance);
        }
    }

    @Test
    void test_fallback() throws InterruptedException {
        try (final Subscription<TerrariaInstanceEntity> subscription = terrariaInstanceSubscriptionService.subscribe(
                TerrariaInstanceFactory.makeInstanceWithId(INSTANCE_ID))) {
            final TerrariaInstanceEntity instance = TerrariaInstanceFactory.makeInstanceWithId(INSTANCE_ID);
            when(terrariaInstanceRepository.getOne(INSTANCE_ID)).thenReturn(instance);
            assertSame(instance, subscription.waitFor(unusedInstance -> true, Duration.ZERO));
        }
    }

    @Test
    void test_generic() throws InterruptedException {
        try (final Subscription<TerrariaInstanceEntity> subscription =
                     terrariaInstanceSubscriptionService.subscribeToAll()) {
            final TerrariaInstanceEntity instance = TerrariaInstanceFactory.makeInstanceWithId(INSTANCE_ID);
            eventPublisher.publishEvent(new TerrariaInstanceApplicationEvent(instance, true));
            assertSame(instance, subscription.waitFor(unusedInstance -> true, Duration.ZERO));
            verify(terrariaInstanceRepository, never()).getOne(anyLong());
        }
    }

    @Test
    void test_message() {
        final TerrariaInstanceEntity instance = TerrariaInstanceFactory.makeInstanceWithId(INSTANCE_ID);
        eventPublisher.publishEvent(new TerrariaInstanceApplicationEvent(instance, true));
        verify(terrariaInstanceMessageService, only()).broadcastInstanceCreation(instance);
    }
}