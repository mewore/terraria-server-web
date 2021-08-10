package io.github.mewore.tsw.services.terraria;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import io.github.mewore.tsw.config.TestConfig;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceAction;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventType;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;

import static io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory.makeInstance;
import static io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory.makeInstanceBuilder;

@Import(TestConfig.class)
@SpringBootTest("spring.h2.console.enabled=true")
class TerrariaMessageServiceIT {

    @Autowired
    private TerrariaMessageService terrariaMessageService;

    @Test
    void testBroadcastInstanceCreation() {
        terrariaMessageService.broadcastInstanceCreation(makeInstance());
    }

    @Test
    void testBroadcastInstanceChange() {
        final TerrariaInstanceEntity instance = makeInstanceBuilder().id(8L)
                .state(TerrariaInstanceState.IDLE)
                .currentAction(TerrariaInstanceAction.BOOT_UP)
                .pendingAction(TerrariaInstanceAction.RUN_SERVER)
                .options(Map.of(1, "option"))
                .build();

        terrariaMessageService.broadcastInstanceChange(instance);
    }

    @Test
    void testBroadcastInstanceDeletion() {
        terrariaMessageService.broadcastInstanceDeletion(makeInstance());
    }

    @Test
    void testBroadcastInstanceEvent() {
        final TerrariaInstanceEventEntity event = TerrariaInstanceEventEntity.builder()
                .type(TerrariaInstanceEventType.INPUT)
                .text("some text")
                .instance(makeInstanceBuilder().id(8L).build())
                .build();

        terrariaMessageService.broadcastInstanceEvent(event);
    }
}