package io.github.mewore.tsw.services.terraria;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.events.Subscription;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;
import io.github.mewore.tsw.models.terraria.TerrariaWorldEntity;
import io.github.mewore.tsw.repositories.file.FileDataRepository;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceEventRepository;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceRepository;
import io.github.mewore.tsw.repositories.terraria.TerrariaWorldRepository;
import io.github.mewore.tsw.services.util.FileService;
import io.github.mewore.tsw.services.util.process.ProcessFailureException;
import io.github.mewore.tsw.services.util.process.ProcessTimeoutException;
import io.github.mewore.tsw.services.util.process.TmuxService;
import lombok.AccessLevel;
import lombok.Getter;
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

    private final TerrariaInstanceRepository terrariaInstanceRepository;

    private final TerrariaInstancePreparationService terrariaInstancePreparationService;

    private final TerrariaInstanceEventRepository terrariaInstanceEventRepository;

    private final TerrariaInstanceEventService terrariaInstanceEventService;

    private final FileDataRepository fileDataRepository;

    private final TerrariaWorldRepository terrariaWorldRepository;

    private final TerrariaWorldService terrariaWorldService;

    private final TerrariaInstanceOutputService terrariaInstanceOutputService;

    private final TerrariaInstanceInputService terrariaInstanceInputService;

    private final TmuxService tmuxService;

    private final FileService fileService;

    private static @Nullable Integer getDesiredModOption(final TerrariaInstanceEntity instance) {
        final Set<String> selectableMods = instance.getOptions()
                .values()
                .stream()
                .map(ModOptionInfo::fromLabel)
                .map(ModOptionInfo::getModName)
                .collect(Collectors.toUnmodifiableSet());
        final List<String> unselectableMods = instance.getModsToEnable()
                .stream()
                .filter(mod -> !selectableMods.contains(mod))
                .sorted()
                .collect(Collectors.toUnmodifiableList());
        if (!unselectableMods.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot enable the following mods because they aren't in the list of known options: " +
                            String.join(", ", unselectableMods));
        }

        for (final Map.Entry<Integer, String> option : instance.getOptions().entrySet()) {
            final ModOptionInfo modOptionInfo = ModOptionInfo.fromLabel(option.getValue());
            if (modOptionInfo.isEnabled() != instance.getModsToEnable().contains(modOptionInfo.getModName())) {
                return option.getKey();
            }
        }
        return null;
    }

    TerrariaInstanceEntity bootUpInstance(TerrariaInstanceEntity instance)
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {

        if (instance.getState() != TerrariaInstanceState.IDLE) {
            throw new IllegalArgumentException("Cannot start an instance with state " + instance.getState());
        }

        terrariaInstancePreparationService.ensureInstanceHasNoOutputFile(instance);

        try (final Subscription<TerrariaInstanceEntity> subscription = terrariaInstanceEventService.subscribe(
                instance)) {
            instance.setNextOutputBytePosition(0L);
            instance = terrariaInstancePreparationService.saveInstance(instance);
            terrariaInstanceOutputService.trackInstance(instance);
            tmuxService.dispatch(instance.getUuid().toString(), instance.getModLoaderServerFile(),
                    instance.getOutputFile());
            return terrariaInstanceEventService.waitForInstanceState(instance, subscription,
                    TerrariaInstanceState.WORLD_MENU, INSTANCE_BOOT_TIMEOUT);
        }
    }

    TerrariaInstanceEntity goToModMenu(final TerrariaInstanceEntity instance)
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {

        if (instance.getState() != TerrariaInstanceState.WORLD_MENU) {
            throw new IllegalArgumentException("Cannot go to the mod menu while in state " + instance.getState());
        }
        return terrariaInstanceInputService.sendInputToInstance(instance, "m", TerrariaInstanceState.MOD_MENU,
                MENU_NAVIGATION_TIMEOUT);
    }

    TerrariaInstanceEntity setInstanceLoadedMods(TerrariaInstanceEntity instance)
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {

        if (instance.getState() != TerrariaInstanceState.MOD_MENU) {
            throw new IllegalArgumentException("Cannot set the mods of an instance with state " + instance.getState());
        }

        @Nullable Integer modOptionToEnter = getDesiredModOption(instance);
        for (int attempt = 0; attempt < MAX_MOD_SETTING_ATTEMPTS && modOptionToEnter != null; attempt++) {
            instance.setState(TerrariaInstanceState.CHANGING_MOD_STATE);
            instance = terrariaInstancePreparationService.saveInstance(instance);
            instance = terrariaInstanceInputService.sendInputToInstance(instance, modOptionToEnter.toString(),
                    TerrariaInstanceState.MOD_MENU, DISABLE_OR_ENABLE_MOD_TIMEOUT);
            modOptionToEnter = getDesiredModOption(instance);
        }
        if (modOptionToEnter != null) {
            throw new RuntimeException(
                    "Failed to make the following mods enabled after " + MAX_MOD_SETTING_ATTEMPTS + " attempts: " +
                            instance.getModsToEnable().stream().sorted().collect(Collectors.joining(", ")));
        }
        instance = terrariaInstanceInputService.sendInputToInstance(instance, "r", TerrariaInstanceState.WORLD_MENU,
                RELOAD_MODS_TIMEOUT);
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
                TerrariaInstanceState.MAX_PLAYERS_PROMPT, MENU_NAVIGATION_TIMEOUT);
        instance = terrariaInstanceInputService.sendInputToInstance(instance, instance.getMaxPlayers().toString(),
                TerrariaInstanceState.PORT_PROMPT, MENU_NAVIGATION_TIMEOUT);
        instance = terrariaInstanceInputService.sendInputToInstance(instance, instance.getPort().toString(),
                TerrariaInstanceState.AUTOMATICALLY_FORWARD_PORT_PROMPT, MENU_NAVIGATION_TIMEOUT);
        instance = terrariaInstanceInputService.sendInputToInstance(instance,
                instance.getAutomaticallyForwardPort() ? "y" : "n", TerrariaInstanceState.PASSWORD_PROMPT,
                MENU_NAVIGATION_TIMEOUT);
        instance = terrariaInstanceInputService.sendInputToInstance(instance, instance.getPassword(),
                TerrariaInstanceState.RUNNING, MENU_NAVIGATION_TIMEOUT.plus(INSTANCE_START_TIMEOUT), true);
        instance.setPassword("");
        return terrariaInstancePreparationService.saveInstance(instance);
    }

    TerrariaInstanceEntity shutDownInstance(TerrariaInstanceEntity instance, final boolean save)
            throws ProcessTimeoutException, InterruptedException, ProcessFailureException {

        if (!instance.getState().isActive()) {
            throw new IllegalArgumentException("Cannot shut down an instance with state " + instance.getState());
        }

        terrariaInstanceOutputService.getInstanceOutputTail(instance).stopReadingFile();
        try {
            if (instance.getState() != TerrariaInstanceState.RUNNING) {
                instance = terrariaInstanceInputService.sendBreakToInstance(instance, TerrariaInstanceState.IDLE,
                        INSTANCE_EXIT_TIMEOUT);
                return instance;
            }
            final @Nullable TerrariaWorldEntity world = instance.getWorld();
            final Set<String> loadedMods = instance.getLoadedMods();
            instance = terrariaInstanceInputService.sendInputToInstance(instance, save ? "exit" : "exit-nosave",
                    TerrariaInstanceState.IDLE,
                    save ? INSTANCE_EXIT_TIMEOUT.plus(INSTANCE_SAVE_TIMEOUT) : INSTANCE_EXIT_TIMEOUT);
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
            return terrariaInstancePreparationService.saveInstance(instance);
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
            instance = terrariaInstanceEventService.waitForInstanceState(instance, subscription,
                    TerrariaInstanceState.IDLE, INSTANCE_EXIT_TIMEOUT);
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

        logger.info("Clearing instance {} events...", instance.getUuid());
        final long deletedEventCount = terrariaInstanceEventRepository.deleteByInstance(instance);
        logger.info("Deleted {} events of instance {}", deletedEventCount, instance.getUuid());

        terrariaInstanceRepository.delete(instance);
        logger.info("Done deleting instance {}", instance.getUuid());
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    private static class ModOptionInfo {

        private static final Pattern LABEL_PATTERN = Pattern.compile("(.+) \\((enabled|disabled)\\)");

        private final boolean enabled;

        private final String modName;

        private static ModOptionInfo fromLabel(final String modOptionLabel) {
            final Matcher matcher = LABEL_PATTERN.matcher(modOptionLabel);
            if (!matcher.find()) {
                throw new IllegalArgumentException("The supposed mod option label '" + modOptionLabel +
                        "' does not match the following pattern: " + LABEL_PATTERN);
            }
            final @Nullable String modOption = matcher.group(1);
            if (modOption == null) {
                throw new RuntimeException(
                        "The pattern '" + LABEL_PATTERN + "' must be invalid because it doesn't have a first group!");
            }
            return new ModOptionInfo("enabled".equals(matcher.group(2)), modOption);
        }
    }
}
