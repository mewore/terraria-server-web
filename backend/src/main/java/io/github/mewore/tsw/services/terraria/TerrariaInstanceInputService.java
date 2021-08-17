package io.github.mewore.tsw.services.terraria;

import java.time.Duration;

import org.springframework.stereotype.Service;

import io.github.mewore.tsw.events.Subscription;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventType;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;
import io.github.mewore.tsw.services.util.process.ProcessFailureException;
import io.github.mewore.tsw.services.util.process.ProcessTimeoutException;
import io.github.mewore.tsw.services.util.process.TmuxService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Service
public class TerrariaInstanceInputService {

    private static final String CTRL_C = "^C";

    private final TerrariaInstanceService terrariaInstanceService;

    private final TerrariaInstanceSubscriptionService terrariaInstanceSubscriptionService;

    private final TmuxService tmuxService;

    public TerrariaInstanceEntity sendBreakToInstance(final TerrariaInstanceEntity instance,
            final Duration timeout,
            final TerrariaInstanceState... expectedResultingStates)
            throws ProcessTimeoutException, InterruptedException, ProcessFailureException {
        return sendInputToInstance(instance, CTRL_C, timeout, false, expectedResultingStates);
    }

    public TerrariaInstanceEntity sendInputToInstance(final TerrariaInstanceEntity instance,
            final String input,
            final Duration timeout,
            final TerrariaInstanceState... expectedResultingStates)
            throws ProcessTimeoutException, InterruptedException, ProcessFailureException {
        return sendInputToInstance(instance, input, timeout, false, expectedResultingStates);
    }

    public TerrariaInstanceEntity sendInputToInstance(final TerrariaInstanceEntity instance,
            final String input,
            final Duration timeout,
            final boolean obfuscateInput,
            final TerrariaInstanceState... expectedResultingStates)
            throws ProcessTimeoutException, InterruptedException, ProcessFailureException {
        if (input.contains("\n")) {
            throw new IllegalArgumentException("Cannot enter an input of multiple lines: " + input);
        }
        final String fullInput = input + "\n";
        final TerrariaInstanceEventEntity instanceEvent = TerrariaInstanceEventEntity.builder()
                .instance(instance)
                .content(obfuscateInput ? "[REDACTED]\n" : fullInput)
                .type(TerrariaInstanceEventType.INPUT)
                .build();

        terrariaInstanceService.saveEvent(instanceEvent);
        try (final Subscription<TerrariaInstanceEntity> subscription = terrariaInstanceSubscriptionService.subscribe(
                instance)) {
            if (input.equals(CTRL_C)) {
                tmuxService.sendCtrlC(instance.getUuid().toString());
            } else {
                tmuxService.sendInput(instance.getUuid().toString(), fullInput);
            }
            return terrariaInstanceSubscriptionService.waitForInstanceState(instance, subscription, timeout,
                    expectedResultingStates);
        }
    }
}
