package io.github.mewore.tsw.services.terraria;

import org.junit.jupiter.api.Test;

import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;

import static io.github.mewore.tsw.services.terraria.TerrariaInstanceOutputEvent.AUTOMATICALLY_FORWARD_PORT;
import static io.github.mewore.tsw.services.terraria.TerrariaInstanceOutputEvent.MAX_PLAYERS;
import static io.github.mewore.tsw.services.terraria.TerrariaInstanceOutputEvent.MOD_BROWSER;
import static io.github.mewore.tsw.services.terraria.TerrariaInstanceOutputEvent.MOD_MENU;
import static io.github.mewore.tsw.services.terraria.TerrariaInstanceOutputEvent.PASSWORD;
import static io.github.mewore.tsw.services.terraria.TerrariaInstanceOutputEvent.PORT;
import static io.github.mewore.tsw.services.terraria.TerrariaInstanceOutputEvent.RUNNING;
import static io.github.mewore.tsw.services.terraria.TerrariaInstanceOutputEvent.WORLD_MENU;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerrariaInstanceOutputEventTest {

    @Test
    void testWorldMenu() {
        final String line = "Choose World: ";
        assertTrue(WORLD_MENU.hasBeenReached(TerrariaInstanceState.BOOTING_UP, line));
        assertTrue(WORLD_MENU.hasBeenReached(TerrariaInstanceState.MOD_MENU, line));
        assertTrue(WORLD_MENU.hasBeenReached(TerrariaInstanceState.MOD_BROWSER, line));
        assertFalse(WORLD_MENU.hasBeenReached(TerrariaInstanceState.BOOTING_UP, "Other prompt: "));
        assertFalse(WORLD_MENU.hasBeenReached(TerrariaInstanceState.BROKEN, line));
    }

    @Test
    void testModMenu() {
        final String line = "Type a command: ";
        assertTrue(MOD_MENU.hasBeenReached(TerrariaInstanceState.WORLD_MENU, line));
        assertTrue(MOD_MENU.hasBeenReached(TerrariaInstanceState.CHANGING_MOD_STATE, line));
        assertFalse(MOD_MENU.hasBeenReached(TerrariaInstanceState.WORLD_MENU, "Other prompt: "));
        assertFalse(MOD_MENU.hasBeenReached(TerrariaInstanceState.BROKEN, line));
    }

    @Test
    void testModBrowser() {
        final String line = "Type an exact ModName to download: ";
        assertTrue(MOD_BROWSER.hasBeenReached(TerrariaInstanceState.WORLD_MENU, line));
        assertFalse(MOD_BROWSER.hasBeenReached(TerrariaInstanceState.WORLD_MENU, "Other prompt: "));
        assertFalse(MOD_BROWSER.hasBeenReached(TerrariaInstanceState.BROKEN, line));
    }

    @Test
    void testMaxPlayers() {
        final String line = "Max players (press enter for 8): ";
        assertTrue(MAX_PLAYERS.hasBeenReached(TerrariaInstanceState.WORLD_MENU, line));
        assertFalse(MAX_PLAYERS.hasBeenReached(TerrariaInstanceState.WORLD_MENU, "Other prompt: "));
        assertFalse(MAX_PLAYERS.hasBeenReached(TerrariaInstanceState.BROKEN, line));
    }

    @Test
    void testPort() {
        final String line = "Server port (press enter for 7777): ";
        assertTrue(PORT.hasBeenReached(TerrariaInstanceState.MAX_PLAYERS_PROMPT, line));
        assertFalse(PORT.hasBeenReached(TerrariaInstanceState.MAX_PLAYERS_PROMPT, "Other prompt: "));
        assertFalse(PORT.hasBeenReached(TerrariaInstanceState.WORLD_MENU, line));
    }

    @Test
    void testAutomaticallyForwardPort() {
        final String line = "Automatically forward port? (y/n): ";
        assertTrue(AUTOMATICALLY_FORWARD_PORT.hasBeenReached(TerrariaInstanceState.PORT_PROMPT, line));
        assertFalse(AUTOMATICALLY_FORWARD_PORT.hasBeenReached(TerrariaInstanceState.PORT_PROMPT, "Other prompt: "));
        assertFalse(AUTOMATICALLY_FORWARD_PORT.hasBeenReached(TerrariaInstanceState.MAX_PLAYERS_PROMPT, line));
    }

    @Test
    void testPassword() {
        final String line = "Server password (press enter for none): ";
        assertTrue(PASSWORD.hasBeenReached(TerrariaInstanceState.AUTOMATICALLY_FORWARD_PORT_PROMPT, line));
        assertFalse(PASSWORD.hasBeenReached(TerrariaInstanceState.AUTOMATICALLY_FORWARD_PORT_PROMPT, "Other prompt: "));
        assertFalse(PASSWORD.hasBeenReached(TerrariaInstanceState.PORT_PROMPT, line));
    }

    @Test
    void testRunning() {
        final String line = "Server started";
        assertTrue(RUNNING.hasBeenReached(TerrariaInstanceState.PASSWORD_PROMPT, line));
        assertFalse(RUNNING.hasBeenReached(TerrariaInstanceState.PASSWORD_PROMPT, "Other line."));
        assertFalse(RUNNING.hasBeenReached(TerrariaInstanceState.AUTOMATICALLY_FORWARD_PORT_PROMPT, line));
    }
}