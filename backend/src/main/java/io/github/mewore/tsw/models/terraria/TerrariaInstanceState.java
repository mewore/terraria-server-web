package io.github.mewore.tsw.models.terraria;

import java.util.Arrays;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static io.github.mewore.tsw.models.terraria.TerrariaInstanceAction.BOOT_UP;
import static io.github.mewore.tsw.models.terraria.TerrariaInstanceAction.CREATE_WORLD;
import static io.github.mewore.tsw.models.terraria.TerrariaInstanceAction.DELETE;
import static io.github.mewore.tsw.models.terraria.TerrariaInstanceAction.GO_TO_MOD_MENU;
import static io.github.mewore.tsw.models.terraria.TerrariaInstanceAction.RECREATE;
import static io.github.mewore.tsw.models.terraria.TerrariaInstanceAction.RUN_SERVER;
import static io.github.mewore.tsw.models.terraria.TerrariaInstanceAction.SET_LOADED_MODS;
import static io.github.mewore.tsw.models.terraria.TerrariaInstanceAction.SET_UP;
import static io.github.mewore.tsw.models.terraria.TerrariaInstanceAction.SHUT_DOWN;
import static io.github.mewore.tsw.models.terraria.TerrariaInstanceAction.SHUT_DOWN_NO_SAVE;
import static io.github.mewore.tsw.models.terraria.TerrariaInstanceAction.TERMINATE;

/**
 * An automaton-based state of a terraria instance.
 */
@RequiredArgsConstructor
public enum TerrariaInstanceState {

    /**
     * Defined by a user but does not exist yet and may not be valid.
     */
    DEFINED(false, SET_UP, DELETE),

    /**
     * Passed the validation checks.
     */
    VALID(false, SET_UP, DELETE),

    /**
     * Ready to be used.
     */
    IDLE(false, BOOT_UP, DELETE),

    /**
     * Waiting for the instance to start.
     */
    BOOTING_UP(true, TERMINATE),

    /**
     * At the main menu, where the worlds are listed (along with the ways to reach other menus).
     */
    WORLD_MENU(true, GO_TO_MOD_MENU, CREATE_WORLD, RUN_SERVER, SHUT_DOWN, TERMINATE),

    /**
     * TSW is changing the state of a mod and waiting for the mod menu to appear again.
     */
    CHANGING_MOD_STATE(true, TERMINATE),

    /**
     * At the menu where the mods are listed.
     */
    MOD_MENU(true, SET_LOADED_MODS, SHUT_DOWN, TERMINATE),

    /**
     * At the menu with mods available for download.
     */
    MOD_BROWSER(true, SHUT_DOWN, TERMINATE),

    /**
     * At the world creation prompt for the world size (small / medium / large).
     */
    WORLD_SIZE_PROMPT(true, SHUT_DOWN, TERMINATE),

    /**
     * At the world creation prompt for the world difficulty (normal / expert).
     */
    WORLD_DIFFICULTY_PROMPT(true, SHUT_DOWN, TERMINATE),

    /**
     * At the world creation prompt for the world name.
     */
    WORLD_NAME_PROMPT(true, SHUT_DOWN, TERMINATE),

    /**
     * At the pre-start prompt for the maximum number of players.
     */
    MAX_PLAYERS_PROMPT(true, SHUT_DOWN, TERMINATE),

    /**
     * At the pre-start prompt for the port number.
     */
    PORT_PROMPT(true, SHUT_DOWN, TERMINATE),

    /**
     * At the pre-start prompt for whether to automatically forward the port.
     */
    AUTOMATICALLY_FORWARD_PORT_PROMPT(true, SHUT_DOWN, TERMINATE),

    /**
     * At the pre-start prompt for the password.
     */
    PASSWORD_PROMPT(true, SHUT_DOWN, TERMINATE),

    /**
     * The server is running and accepting connections from players.
     */
    RUNNING(true, SHUT_DOWN, SHUT_DOWN_NO_SAVE, TERMINATE),

    /**
     * The server has been run while another one on the same host was using the same port. It should get shut down
     * automatically afterwards.
     */
    PORT_CONFLICT(true, SHUT_DOWN, TERMINATE),

    /**
     * The instance has failed validation checks. This is assumed to be due to its configuration being incorrect.
     */
    INVALID(false, DELETE),

    /**
     * The instance has encountered an unknown exception. Its real state is unknown, and it may be impossible to use.
     */
    BROKEN(false, RECREATE, DELETE);

    /**
     * Whether the instance is expected/assumed to be running as a process while it is in this state.
     */
    @Getter
    private final boolean active;

    /**
     * The actions that can be executed on an instance in this state.
     */
    @Getter
    private final Set<TerrariaInstanceAction> applicableActions;

    @JsonIgnore
    @Getter
    private final String[] applicableActionNames;

    TerrariaInstanceState(final boolean active, final TerrariaInstanceAction... applicableActionArray) {
        this.active = active;
        applicableActions = Set.of(applicableActionArray);
        applicableActionNames = Arrays.stream(applicableActionArray).map(Enum::name).toArray(String[]::new);
    }

    boolean isActionApplicable(final TerrariaInstanceAction action) {
        return applicableActions.contains(action);
    }
}
