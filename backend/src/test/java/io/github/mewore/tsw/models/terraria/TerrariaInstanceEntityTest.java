package io.github.mewore.tsw.models.terraria;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.mewore.tsw.models.file.OperatingSystem;

import static io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory.makeInstance;
import static io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory.makeInstanceBuilder;
import static io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory.makeInstanceWithState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class TerrariaInstanceEntityTest {

    @Test
    void testGetModLoaderServerFile_windows() {
        final TerrariaInstanceEntity instance = makeInstance();
        when(instance.getHost().getOs()).thenReturn(OperatingSystem.WINDOWS);
        assertEquals("tModLoaderServer.exe", instance.getModLoaderServerFile().getName());
    }

    @Test
    void testGetModLoaderServerFile_linux() {
        final TerrariaInstanceEntity instance = makeInstance();
        when(instance.getHost().getOs()).thenReturn(OperatingSystem.LINUX);
        assertEquals("tModLoaderServer", instance.getModLoaderServerFile().getName());
    }

    @Test
    void testGetOptionKey() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.BOOTING_UP);
        instance.acknowledgeMenuOption(1, "Option1");
        instance.acknowledgeMenuOption(2, "Option2");
        instance.setState(TerrariaInstanceState.WORLD_MENU);

        assertEquals(2, instance.getOptionKey("Option2"));
    }

    @Test
    void testGetOptionKey_noOption() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.BOOTING_UP);
        instance.acknowledgeMenuOption(1, "Option1");
        instance.acknowledgeMenuOption(2, "Option2");
        instance.setState(TerrariaInstanceState.WORLD_MENU);

        final Exception exception = assertThrows(IllegalArgumentException.class,
                () -> instance.getOptionKey("Option3"));
        assertEquals("There is no option \"Option3\" in the known options:\n1\t\tOption1\n2\t\tOption2",
                exception.getMessage());
    }

    @Test
    void testSetState() {
        final TerrariaInstanceEntity instance = makeInstanceBuilder().state(TerrariaInstanceState.IDLE)
                .pendingOptions(Map.of(1, "PendingOption1"))
                .options(Map.of(1, "Option1"))
                .build();
        instance.setState(TerrariaInstanceState.BOOTING_UP);
        assertSame(TerrariaInstanceState.BOOTING_UP, instance.getState());
        assertEquals(Collections.emptyMap(), instance.getPendingOptions());
        assertEquals(Map.of(1, "PendingOption1"), instance.getOptions());
    }

    @Test
    void testSetState_same() {
        final TerrariaInstanceEntity instance = makeInstanceBuilder().state(TerrariaInstanceState.BOOTING_UP)
                .pendingOptions(Map.of(1, "PendingOption1"))
                .options(Map.of(1, "Option1"))
                .build();
        instance.setState(TerrariaInstanceState.BOOTING_UP);
        assertEquals(Map.of(1, "PendingOption1"), instance.getPendingOptions());
        assertEquals(Map.of(1, "Option1"), instance.getOptions());
    }

    @Test
    void testSetState_inactive() {
        final TerrariaInstanceEntity instance = makeInstanceBuilder().state(TerrariaInstanceState.BOOTING_UP)
                .pendingOptions(Map.of(1, "PendingOption1"))
                .options(Map.of(1, "Option1"))
                .build();
        instance.setState(TerrariaInstanceState.IDLE);
        assertEquals(Collections.emptyMap(), instance.getPendingOptions());
        assertEquals(Collections.emptyMap(), instance.getOptions());
    }

    @Test
    void testStartAction() {
        final TerrariaInstanceEntity instance = makeInstanceBuilder().state(TerrariaInstanceState.IDLE)
                .pendingAction(TerrariaInstanceAction.BOOT_UP)
                .actionExecutionStartTime(null)
                .build();
        instance.startAction();
        assertSame(TerrariaInstanceAction.BOOT_UP, instance.getCurrentAction());
        assertNull(instance.getPendingAction());
        assertNotNull(instance.getActionExecutionStartTime());
    }

    @Test
    void testCompleteAction() {
        final TerrariaInstanceEntity instance = makeInstanceBuilder().state(TerrariaInstanceState.IDLE)
                .currentAction(TerrariaInstanceAction.BOOT_UP)
                .actionExecutionStartTime(Instant.now())
                .build();
        instance.completeAction();
        assertNull(instance.getCurrentAction());
        assertNull(instance.getActionExecutionStartTime());
    }

    @Test
    void testAcknowledgeMenuOption() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.IDLE);
        instance.acknowledgeMenuOption(1, "Option");
        assertEquals(Map.of(1, "Option"), instance.getPendingOptions());
        assertEquals(Collections.emptyMap(), instance.getOptions());
    }
}