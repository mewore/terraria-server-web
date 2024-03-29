package io.github.mewore.tsw.services.terraria;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mewore.tsw.events.FakeSubscription;
import io.github.mewore.tsw.events.Subscription;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;
import io.github.mewore.tsw.services.util.process.ProcessFailureException;
import io.github.mewore.tsw.services.util.process.ProcessTimeoutException;
import io.github.mewore.tsw.services.util.process.TmuxService;

import static io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory.INSTANCE_UUID;
import static io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory.makeInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerrariaInstanceInputServiceTest {

    @InjectMocks
    private TerrariaInstanceInputService terrariaInstanceInputService;

    @Mock
    private TerrariaInstanceService terrariaInstanceService;

    @Mock
    private TerrariaInstanceSubscriptionService terrariaInstanceSubscriptionService;

    @Mock
    private TmuxService tmuxService;

    @Captor
    private ArgumentCaptor<TerrariaInstanceEventEntity> eventCaptor;

    @Test
    void testSendBreakToInstance() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstance();
        when(terrariaInstanceSubscriptionService.subscribe(instance)).thenReturn(new FakeSubscription<>());
        when(terrariaInstanceSubscriptionService.waitForInstanceState(same(instance), any(), any(), any())).thenReturn(
                instance);

        terrariaInstanceInputService.sendBreakToInstance(instance, Duration.ofMinutes(10), TerrariaInstanceState.IDLE);
        verify(terrariaInstanceService).saveEvent(eventCaptor.capture());
        assertEquals("^C\n", eventCaptor.getValue().getContent());
        verify(tmuxService, only()).sendCtrlC(INSTANCE_UUID.toString());
    }

    @Test
    void testSendInputToInstance() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstance();
        final Subscription<TerrariaInstanceEntity> subscription = new FakeSubscription<>();
        when(terrariaInstanceSubscriptionService.subscribe(instance)).thenReturn(subscription);

        final TerrariaInstanceEntity awaitedInstance = mock(TerrariaInstanceEntity.class);
        when(terrariaInstanceSubscriptionService.waitForInstanceState(instance, subscription, Duration.ofMinutes(10),
                TerrariaInstanceState.IDLE)).thenReturn(awaitedInstance);

        final TerrariaInstanceEntity result = terrariaInstanceInputService.sendInputToInstance(instance, "input",
                Duration.ofMinutes(10), TerrariaInstanceState.IDLE);
        assertSame(awaitedInstance, result);

        verify(terrariaInstanceService).saveEvent(eventCaptor.capture());
        assertEquals("input\n", eventCaptor.getValue().getContent());
        verify(tmuxService, only()).sendInput(INSTANCE_UUID.toString(), "input\n");
    }

    @Test
    void testSendInputToInstance_obfuscated()
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstance();
        when(terrariaInstanceSubscriptionService.subscribe(instance)).thenReturn(new FakeSubscription<>());
        when(terrariaInstanceSubscriptionService.waitForInstanceState(same(instance), any(), any(), any())).thenReturn(
                instance);

        terrariaInstanceInputService.sendInputToInstance(instance, "sensitive input", Duration.ofMinutes(10), true,
                TerrariaInstanceState.IDLE);
        verify(terrariaInstanceService).saveEvent(eventCaptor.capture());
        assertEquals("[REDACTED]\n", eventCaptor.getValue().getContent());
        verify(tmuxService, only()).sendInput(INSTANCE_UUID.toString(), "sensitive input\n");
    }

    @Test
    void testSendInputToInstance_newline() {
        final TerrariaInstanceEntity instance = makeInstance();
        final Exception exception = assertThrows(IllegalArgumentException.class,
                () -> terrariaInstanceInputService.sendInputToInstance(instance, "input\nmore input",
                        Duration.ofMinutes(10), TerrariaInstanceState.IDLE));
        assertEquals("Cannot enter an input of multiple lines: input\nmore input", exception.getMessage());
    }
}