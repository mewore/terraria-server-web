package io.github.mewore.tsw.services.terraria;

import java.time.Duration;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.events.Subscription;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceAction;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;
import io.github.mewore.tsw.models.terraria.TerrariaWorldEntity;
import io.github.mewore.tsw.repositories.file.FileDataRepository;
import io.github.mewore.tsw.repositories.terraria.TerrariaWorldRepository;
import io.github.mewore.tsw.services.util.FileService;
import io.github.mewore.tsw.services.util.process.ProcessFailureException;
import io.github.mewore.tsw.services.util.process.ProcessTimeoutException;
import io.github.mewore.tsw.services.util.process.TmuxService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Service
public class TerrariaInstanceExecutionService {

    private static final int MAX_MOD_SETTING_ATTEMPTS = 100;

    private static final Duration INSTANCE_EXIT_TIMEOUT = Duration.ofSeconds(30);

    private static final Duration INSTANCE_SAVE_TIMEOUT = Duration.ofMinutes(1);

    private static final Duration INSTANCE_BOOT_TIMEOUT = Duration.ofMinutes(1);

    private static final Duration MENU_NAVIGATION_TIMEOUT = Duration.ofSeconds(10);

    private static final Duration INSTANCE_START_TIMEOUT = Duration.ofMinutes(3);

    private static final Duration DISABLE_OR_ENABLE_MOD_TIMEOUT = Duration.ofSeconds(30);

    private static final Duration RELOAD_MODS_TIMEOUT = Duration.ofMinutes(2);

    private final Logger logger = LogManager.getLogger(getClass());

    private final TerrariaInstanceService terrariaInstanceService;

    private final TerrariaInstanceEventService terrariaInstanceEventService;

    private final FileDataRepository fileDataRepository;

    private final TerrariaWorldRepository terrariaWorldRepository;

    private final TerrariaWorldService terrariaWorldService;

    private final TerrariaInstanceOutputService terrariaInstanceOutputService;

    private final TerrariaInstanceInputService terrariaInstanceInputService;

    private final TmuxService tmuxService;

    private final FileService fileService;

    TerrariaInstanceEntity bootUpInstance(TerrariaInstanceEntity instance)
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {

        if (instance.getState() != TerrariaInstanceState.IDLE) {
            throw new IllegalArgumentException("Cannot start an instance with state " + instance.getState());
        }

        terrariaInstanceService.ensureInstanceHasNoOutputFile(instance);

        try (final Subscription<TerrariaInstanceEntity> subscription = terrariaInstanceEventService.subscribe(
                instance)) {
            instance.setNextOutputBytePosition(0L);
            instance = terrariaInstanceService.saveInstance(instance);
            terrariaInstanceOutputService.trackInstance(instance);
            tmuxService.dispatch(instance.getUuid().toString(), instance.getModLoaderServerFile(),
                    instance.getOutputFile());
            return terrariaInstanceEventService.waitForInstanceState(instance, subscription, INSTANCE_BOOT_TIMEOUT,
                    TerrariaInstanceState.WORLD_MENU);
        }
    }

    TerrariaInstanceEntity goToModMenu(final TerrariaInstanceEntity instance)
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {

        if (instance.getState() != TerrariaInstanceState.WORLD_MENU) {
            throw new IllegalArgumentException("Cannot go to the mod menu while in state " + instance.getState());
        }
        return terrariaInstanceInputService.sendInputToInstance(instance, "m", MENU_NAVIGATION_TIMEOUT,
                TerrariaInstanceState.MOD_MENU);
    }

    TerrariaInstanceEntity setInstanceLoadedMods(TerrariaInstanceEntity instance)
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {

        if (instance.getState() != TerrariaInstanceState.MOD_MENU) {
            throw new IllegalArgumentException("Cannot set the mods of an instance with state " + instance.getState());
        }

        @Nullable Integer modOptionToEnter = terrariaInstanceService.getDesiredModOption(instance);
        for (int attempt = 0; attempt < MAX_MOD_SETTING_ATTEMPTS && modOptionToEnter != null; attempt++) {
            instance.setState(TerrariaInstanceState.CHANGING_MOD_STATE);
            instance = terrariaInstanceService.saveInstance(instance);
            instance = terrariaInstanceInputService.sendInputToInstance(instance, modOptionToEnter.toString(),
                    DISABLE_OR_ENABLE_MOD_TIMEOUT, TerrariaInstanceState.MOD_MENU);
            modOptionToEnter = terrariaInstanceService.getDesiredModOption(instance);
        }
        if (modOptionToEnter != null) {
            throw new RuntimeException(
                    "Failed to make the following mods enabled after " + MAX_MOD_SETTING_ATTEMPTS + " attempts: " +
                            instance.getModsToEnable().stream().sorted().collect(Collectors.joining(", ")));
        }
        instance = terrariaInstanceInputService.sendInputToInstance(instance, "r", RELOAD_MODS_TIMEOUT,
                TerrariaInstanceState.WORLD_MENU);
        if (instance.getLoadedMods().size() != instance.getModsToEnable().size()) {
            throw new RuntimeException(String.format(
                    "The mods of instance %s (%d: %s) are not exactly as many as the requested ones (%d: %s)",
                    instance.getUuid(), instance.getLoadedMods().size(),
                    instance.getLoadedMods().stream().sorted().collect(Collectors.joining(", ")),
                    instance.getModsToEnable().size(),
                    instance.getModsToEnable().stream().sorted().collect(Collectors.joining(", "))));
        }
        return instance;
    }

    TerrariaInstanceEntity runInstance(TerrariaInstanceEntity instance)
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {

        if (instance.getState() != TerrariaInstanceState.WORLD_MENU) {
            throw new IllegalArgumentException("Cannot run an instance with state " + instance.getState());
        }
        final @Nullable TerrariaWorldEntity world = instance.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("Cannot run an instance that does not have an assigned world!");
        }
        if (instance.getOptions().isEmpty()) {
            throw new IllegalArgumentException("Cannot run an instance that does not have any options!");
        }
        final @Nullable Integer worldMenuOption = instance.getOptions()
                .entrySet()
                .stream()
                .filter(option -> option.getValue().equals(world.getName()))
                .findAny()
                .map(Map.Entry::getKey)
                .orElse(null);
        if (worldMenuOption == null) {
            throw new IllegalArgumentException(String.format(
                    "Cannot run instance %s with world %s because it isn't in the known menu world options:\n%s",
                    instance.getUuid(), world.getName(), instance.getOptions()
                            .entrySet()
                            .stream()
                            .sorted(Comparator.comparingInt(Map.Entry::getKey))
                            .map(option -> option.getKey() + "\t\t" + option.getValue())
                            .collect(Collectors.joining("\n"))));
        }
        final @Nullable Set<String> worldMods = world.getMods();
        if (!instance.getLoadedMods().equals(worldMods)) {
            logger.warn("The mods of instance {} ({}) are different from the ones of world {} ({})", instance.getUuid(),
                    String.join(", ", instance.getLoadedMods()), world.getName(),
                    worldMods == null ? "UNKNOWN" : String.join(", ", worldMods));
        }

        instance = terrariaInstanceInputService.sendInputToInstance(instance, worldMenuOption.toString(),
                MENU_NAVIGATION_TIMEOUT, TerrariaInstanceState.MAX_PLAYERS_PROMPT);
        instance = terrariaInstanceInputService.sendInputToInstance(instance, instance.getMaxPlayers().toString(),
                MENU_NAVIGATION_TIMEOUT, TerrariaInstanceState.PORT_PROMPT);
        instance = terrariaInstanceInputService.sendInputToInstance(instance, instance.getPort().toString(),
                MENU_NAVIGATION_TIMEOUT, TerrariaInstanceState.AUTOMATICALLY_FORWARD_PORT_PROMPT);
        instance = terrariaInstanceInputService.sendInputToInstance(instance,
                instance.getAutomaticallyForwardPort() ? "y" : "n", MENU_NAVIGATION_TIMEOUT,
                TerrariaInstanceState.PASSWORD_PROMPT);
        instance = terrariaInstanceInputService.sendInputToInstance(instance, instance.getPassword(),
                MENU_NAVIGATION_TIMEOUT.plus(INSTANCE_START_TIMEOUT), true, TerrariaInstanceState.RUNNING,
                TerrariaInstanceState.PORT_CONFLICT);
        instance.setPassword("");
        if (instance.getState() == TerrariaInstanceState.PORT_CONFLICT) {
            instance.setPendingAction(TerrariaInstanceAction.SHUT_DOWN);
        }
        return terrariaInstanceService.saveInstance(instance);
    }

    TerrariaInstanceEntity shutDownInstance(TerrariaInstanceEntity instance, final boolean save)
            throws ProcessTimeoutException, InterruptedException, ProcessFailureException {

        if (!instance.getState().isActive()) {
            throw new IllegalArgumentException("Cannot shut down an instance with state " + instance.getState());
        }

        terrariaInstanceOutputService.getInstanceOutputTail(instance).stopReadingFile();
        try {
            if (instance.getState() != TerrariaInstanceState.RUNNING) {
                instance = terrariaInstanceInputService.sendBreakToInstance(instance, INSTANCE_EXIT_TIMEOUT,
                        TerrariaInstanceState.IDLE);
                return instance;
            }
            final @Nullable TerrariaWorldEntity world = instance.getWorld();
            final Set<String> loadedMods = instance.getLoadedMods();
            instance = terrariaInstanceInputService.sendInputToInstance(instance, save ? "exit" : "exit-nosave",
                    save ? INSTANCE_EXIT_TIMEOUT.plus(INSTANCE_SAVE_TIMEOUT) : INSTANCE_EXIT_TIMEOUT,
                    TerrariaInstanceState.IDLE);
            if (save) {
                if (world == null) {
                    logger.warn("The instance {} does not have a world despite actively running", instance.getUuid());
                } else {
                    final @Nullable TerrariaWorldEntity newWorld = terrariaWorldService.readWorld(world);
                    if (newWorld == null) {
                        logger.warn("Failed to read the new world data of {}. Skipping saving it.", instance.getUuid());
                    } else {
                        world.setMods(loadedMods);
                        world.setData(fileDataRepository.save(newWorld.getData()));
                        world.setLastModified(newWorld.getLastModified());
                        terrariaWorldRepository.save(world);
                    }
                }
            }
            return instance;
        } finally {
            terrariaInstanceOutputService.stopTrackingInstance(instance);
        }
    }

    TerrariaInstanceEntity terminateInstance(TerrariaInstanceEntity instance)
            throws ProcessTimeoutException, InterruptedException, ProcessFailureException {

        if (!instance.getState().isActive()) {
            throw new IllegalArgumentException("Cannot terminate an instance with state " + instance.getState());
        }

        if (!tmuxService.hasSession(instance.getUuid().toString())) {
            logger.warn("The instance {} is already not running while trying to terminate it", instance.getUuid());
            terrariaInstanceOutputService.stopTrackingInstance(instance);
            instance.setState(TerrariaInstanceState.IDLE);
            return terrariaInstanceService.saveInstance(instance);
        }

        if (!terrariaInstanceOutputService.isTrackingInstance(instance)) {
            logger.warn("Not tracking instance {}! Tracking it now in order to terminate it properly...",
                    instance.getUuid());
            terrariaInstanceOutputService.trackInstance(instance);
        }

        terrariaInstanceOutputService.getInstanceOutputTail(instance).stopReadingFile();
        try (final Subscription<TerrariaInstanceEntity> subscription = terrariaInstanceEventService.subscribe(
                instance)) {
            tmuxService.kill(instance.getUuid().toString());
            instance = terrariaInstanceEventService.waitForInstanceState(instance, subscription, INSTANCE_EXIT_TIMEOUT,
                    TerrariaInstanceState.IDLE);
        } finally {
            terrariaInstanceOutputService.stopTrackingInstance(instance);
        }
        return instance;
    }

    void deleteInstance(final TerrariaInstanceEntity instance)
            throws InterruptedException, ProcessFailureException, ProcessTimeoutException {

        if (instance.getState().isActive()) {
            throw new IllegalArgumentException(
                    String.format("Cannot delete a running instance (with state %s)!", instance.getState()));
        }

        terrariaInstanceOutputService.stopTrackingInstance(instance);

        if (tmuxService.hasSession(instance.getUuid().toString())) {
            logger.warn("The instance {} seems to have a rogue tmux session. Killing it...", instance.getUuid());
            tmuxService.kill(instance.getUuid().toString());
        }

        logger.info("Deleting instance {}...", instance.getUuid());
        if (!fileService.fileExists(instance.getLocation())) {
            logger.warn("The directory of instance {} already does not exist", instance.getUuid());
        } else if (!fileService.deleteRecursively(instance.getLocation())) {
            logger.warn(
                    "Failed to delete the directory of instance {}. Proceeding with its deletion from the database " +
                            "anyway...", instance.getUuid());
        }

        terrariaInstanceService.deleteInstance(instance);
    }
}
