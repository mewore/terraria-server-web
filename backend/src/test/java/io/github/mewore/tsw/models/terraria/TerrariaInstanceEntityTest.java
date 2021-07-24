package io.github.mewore.tsw.models.terraria;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class TerrariaInstanceEntityTest {

    private static TerrariaInstanceEntity.TerrariaInstanceEntityBuilder makeInstance(final TerrariaInstanceState state) {
        return TerrariaInstanceFactory.makeInstanceBuilder().state(state);
    }

    @Test
    void testSetState() {
        final TerrariaInstanceEntity instance = makeInstance(TerrariaInstanceState.IDLE).pendingOptions(
                Map.of(1, "PendingOption1")).options(Map.of(1, "Option1")).build();
        instance.setState(TerrariaInstanceState.BOOTING_UP);
        assertSame(TerrariaInstanceState.BOOTING_UP, instance.getState());
        assertEquals(Collections.emptyMap(), instance.getPendingOptions());
        assertEquals(Map.of(1, "PendingOption1"), instance.getOptions());
    }

    @Test
    void testSetState_same() {
        final TerrariaInstanceEntity instance = makeInstance(TerrariaInstanceState.BOOTING_UP).pendingOptions(
                Map.of(1, "PendingOption1")).options(Map.of(1, "Option1")).build();
        instance.setState(TerrariaInstanceState.BOOTING_UP);
        assertEquals(Map.of(1, "PendingOption1"), instance.getPendingOptions());
        assertEquals(Map.of(1, "Option1"), instance.getOptions());
    }

    @Test
    void testSetState_inactive() {
        final TerrariaInstanceEntity instance = makeInstance(TerrariaInstanceState.BOOTING_UP).pendingOptions(
                Map.of(1, "PendingOption1")).options(Map.of(1, "Option1")).build();
        instance.setState(TerrariaInstanceState.IDLE);
        assertEquals(Collections.emptyMap(), instance.getPendingOptions());
        assertEquals(Collections.emptyMap(), instance.getOptions());
    }

    @Test
    void testStartAction() {
        final TerrariaInstanceEntity instance = makeInstance(TerrariaInstanceState.IDLE).pendingAction(
                TerrariaInstanceAction.BOOT_UP).actionExecutionStartTime(null).build();
        instance.startAction();
        assertSame(TerrariaInstanceAction.BOOT_UP, instance.getCurrentAction());
        assertNull(instance.getPendingAction());
        assertNotNull(instance.getActionExecutionStartTime());
    }

    @Test
    void testCompleteAction() {
        final TerrariaInstanceEntity instance = makeInstance(TerrariaInstanceState.IDLE).currentAction(
                TerrariaInstanceAction.BOOT_UP).actionExecutionStartTime(Instant.now()).build();
        instance.completeAction();
        assertNull(instance.getCurrentAction());
        assertNull(instance.getActionExecutionStartTime());
    }

    @Test
    void testAcknowledgeMenuOption() {
        final TerrariaInstanceEntity instance = makeInstance(TerrariaInstanceState.IDLE).build();
        instance.acknowledgeMenuOption(1, "Option");
        assertEquals(Map.of(1, "Option"), instance.getPendingOptions());
        assertEquals(Collections.emptyMap(), instance.getOptions());
    }
}