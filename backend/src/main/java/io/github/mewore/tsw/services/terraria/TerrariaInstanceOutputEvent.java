package io.github.mewore.tsw.services.terraria;

import java.util.Set;

import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TerrariaInstanceOutputEvent {

    WORLD_MENU(TerrariaInstanceState.WORLD_MENU, "Choose World:", TerrariaInstanceState.BOOTING_UP,
            TerrariaInstanceState.MOD_MENU, TerrariaInstanceState.MOD_BROWSER),
    MOD_MENU(TerrariaInstanceState.MOD_MENU, "Type a command:", TerrariaInstanceState.WORLD_MENU,
            TerrariaInstanceState.CHANGING_MOD_STATE),
    MOD_BROWSER(TerrariaInstanceState.MOD_BROWSER, "Type an exact ModName to download:",
            TerrariaInstanceState.WORLD_MENU),
    MAX_PLAYERS(TerrariaInstanceState.MAX_PLAYERS_PROMPT, "Max players (press enter for 8):",
            TerrariaInstanceState.WORLD_MENU),
    PORT(TerrariaInstanceState.PORT_PROMPT, "Server port (press enter for 7777):",
            TerrariaInstanceState.MAX_PLAYERS_PROMPT),
    AUTOMATICALLY_FORWARD_PORT(TerrariaInstanceState.AUTOMATICALLY_FORWARD_PORT_PROMPT,
            "Automatically forward port? (y/n):", TerrariaInstanceState.PORT_PROMPT),
    PASSWORD(TerrariaInstanceState.PASSWORD_PROMPT, "Server password (press enter for none):",
            TerrariaInstanceState.AUTOMATICALLY_FORWARD_PORT_PROMPT),
    PORT_CONFLICT(TerrariaInstanceState.PORT_CONFLICT, "Tried to run two servers on the same PC",
            TerrariaInstanceState.PASSWORD_PROMPT),
    RUNNING(TerrariaInstanceState.RUNNING, "Listening on port", TerrariaInstanceState.PASSWORD_PROMPT);

    @Getter
    private final TerrariaInstanceState targetState;

    private final String linePrefix;

    private final Set<TerrariaInstanceState> sourceStates;

    TerrariaInstanceOutputEvent(final TerrariaInstanceState targetState,
            final String linePrefix,
            final TerrariaInstanceState... sourceStates) {
        this.targetState = targetState;
        this.linePrefix = linePrefix;
        this.sourceStates = Set.of(sourceStates);
    }

    public boolean hasBeenReached(final TerrariaInstanceState sourceState, final String line) {
        return sourceStates.contains(sourceState) && line.startsWith(linePrefix);
    }
}
