package io.github.mewore.tsw.services.terraria;

import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventType;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceEventRepository;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceRepository;
import io.github.mewore.tsw.services.util.FileService;
import io.github.mewore.tsw.services.util.FileTail;
import io.github.mewore.tsw.services.util.FileTailEventConsumer;
import io.github.mewore.tsw.services.util.process.ProcessFailureException;
import io.github.mewore.tsw.services.util.process.ProcessTimeoutException;
import io.github.mewore.tsw.services.util.process.TmuxService;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Service
public class TerrariaInstanceOutputService {

    private static final Set<String> MOD_CLEAR_LINES = Set.of("Unloading mods...", "Finding Mods...",
            "Instantiating Mods...");

    private static final Pattern MOD_LOADING_PATTERN = Pattern.compile("^Loading: (.+ v[\\d.]+)$");

    private final Logger logger = LogManager.getLogger(getClass());

    private final FileService fileService;

    private final TerrariaInstanceRepository terrariaInstanceRepository;

    private final TerrariaInstanceEventRepository terrariaInstanceEventRepository;

    private final TerrariaInstancePreparationService terrariaInstancePreparationService;

    private final TmuxService tmuxService;

    private final Map<Long, FileTail> outputTailMap = new HashMap<>();

    public void trackInstance(final TerrariaInstanceEntity instance) {
        if (outputTailMap.containsKey(instance.getId())) {
            logger.warn("The instance {} is already being tracked", instance.getId());
            return;
        }
        final long startPosition = instance.getNextOutputBytePosition();
        final FileTailEventConsumer eventConsumer = new TerrariaOutputEventConsumer(instance);
        final FileTail tail = fileService.tail(instance.getOutputFile(), startPosition, eventConsumer);
        outputTailMap.put(instance.getId(), tail);
    }

    public boolean isTrackingInstance(final TerrariaInstanceEntity instance) {
        return outputTailMap.containsKey(instance.getId());
    }

    public FileTail getInstanceOutputTail(final TerrariaInstanceEntity instance) {
        final @Nullable FileTail result = outputTailMap.get(instance.getId());
        if (result == null) {
            throw new IllegalStateException(
                    String.format("The output of instance %s is not being tracked", instance.getUuid()));
        }
        return result;
    }

    public void stopTrackingInstance(final TerrariaInstanceEntity instance) {
        final @Nullable FileTail tail = outputTailMap.get(instance.getId());
        if (tail == null) {
            logger.info("The instance {} is already not being tracked", instance.getId());
            return;
        }
        tail.stop();
        outputTailMap.remove(instance.getId());
    }

    @Transactional
    private TerrariaInstanceEntity saveInstanceAndEvent(final TerrariaInstanceEntity instance,
            final TerrariaInstanceEventEntity event) {
        terrariaInstanceEventRepository.save(event);
        return terrariaInstancePreparationService.saveInstance(instance);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    private static class WorldMenuOption {

        private static final Pattern WORLD_MENU_OPTION_PATTERN = Pattern.compile("^(\\d+)\t\t(.+)$");

        private final String label;

        private final int id;

        public static @Nullable WorldMenuOption fromLine(final String line) {
            final Matcher optionMatcher = WORLD_MENU_OPTION_PATTERN.matcher(line);
            if (!optionMatcher.find()) {
                return null;
            }
            final @Nullable String firstGroup = optionMatcher.group(1);
            final @Nullable String secondGroup = optionMatcher.group(2);
            if (firstGroup == null || secondGroup == null) {
                return null;
            }
            return new WorldMenuOption(secondGroup, Integer.parseInt(firstGroup));
        }
    }

    @RequiredArgsConstructor
    private class TerrariaOutputEventConsumer implements FileTailEventConsumer {

        private final Logger logger = LogManager.getLogger(getClass());

        private StringBuilder lineBuffer = new StringBuilder();

        private StringBuilder outputTextBuffer = new StringBuilder();

        private @NonNull TerrariaInstanceEntity instance;

        @Override
        public void onFileCreated() {
            onFileExistenceChanged(true);
        }

        @Override
        public void onReadStarted() {
            instance = terrariaInstanceRepository.getOne(instance.getId());
        }

        @Override
        public void onCharacter(final char character, final long position) {
            outputTextBuffer.append(character);
            if (character == '\n') {
                if (instance.getState() != TerrariaInstanceState.RUNNING) {
                    applyLineToInstance(lineBuffer.toString());
                }
                lineBuffer = new StringBuilder();
                instance.setNextOutputBytePosition(position + 1);
            } else {
                lineBuffer.append(character);
            }
        }

        @Override
        public void onReadFinished(final long endPosition) {
            final String allText = outputTextBuffer.toString();
            if (allText.isEmpty()) {
                return;
            }
            outputTextBuffer = new StringBuilder();
            logger.info("Reached the end of the output file. Text:\n<{}>", allText);

            if (instance.getState() != TerrariaInstanceState.RUNNING &&
                    applyLineToInstanceState(lineBuffer.toString())) {
                lineBuffer = new StringBuilder();
                instance.setNextOutputBytePosition(endPosition);
            }

            final TerrariaInstanceEventEntity logSegment = TerrariaInstanceEventEntity.builder()
                    .type(TerrariaInstanceEventType.OUTPUT)
                    .text(allText.replaceAll(
                            "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{1,6} is connecting\\.\\.\\.",
                            "[REDACTED IP] is connecting..."))
                    .instance(instance)
                    .build();
            instance = saveInstanceAndEvent(instance, logSegment);
        }

        @Override
        public void onFileDeleted() {
            onFileExistenceChanged(false);
        }

        @Override
        public void close() {
        }

        /**
         * Modify the current instance based on a line.
         *
         * @param line The line to apply to the instance.
         */
        private void applyLineToInstance(final String line) {
            if (!applyLineToInstanceState(line) && !applyLineToInstanceMods(line)) {
                applyLineToInstanceOptions(line);
            }
        }

        private boolean applyLineToInstanceState(final String line) {
            final @Nullable TerrariaInstanceState newState = Arrays.stream(TerrariaInstanceOutputEvent.values())
                    .filter(transition -> transition.hasBeenReached(instance.getState(), line))
                    .findAny()
                    .map(TerrariaInstanceOutputEvent::getTargetState)
                    .orElse(null);
            if (newState == null) {
                return false;
            }
            logger.info("Instance {} has transitioned from state {} into state {}", instance.getUuid(),
                    instance.getState(), newState);
            instance.setState(newState);
            return true;
        }

        private boolean applyLineToInstanceMods(final String line) {
            if (MOD_CLEAR_LINES.contains(line)) {
                logger.info("The mods of instance {} are now NONE.", instance.getUuid());
                instance.setLoadedMods(Collections.emptySet());
                return true;
            }
            final Matcher modMatcher = MOD_LOADING_PATTERN.matcher(line);
            if (!modMatcher.find()) {
                return false;
            }
            final @Nullable String modName = modMatcher.group(1);
            if (modName == null) {
                logger.warn("Expected to find a mod name in the following line: {}", line);
                return false;
            }
            if (modName.equals("ModLoader v" + instance.getModLoaderVersion()) ||
                    instance.getLoadedMods().contains(modName)) {
                return true;
            }
            final Set<String> newMods = new HashSet<>(instance.getLoadedMods());
            newMods.add(modName);
            logger.info("The mods of instance {} are now: {}", instance.getUuid(), String.join(", ", newMods));
            instance.setLoadedMods(newMods);
            return true;
        }

        private void applyLineToInstanceOptions(final String line) {
            final @Nullable WorldMenuOption newOption = WorldMenuOption.fromLine(line);
            if (newOption == null) {
                return;
            }
            final @Nullable String existingOptionLabel = instance.getPendingOptions().get(newOption.getId());
            if (existingOptionLabel != null) {
                if (newOption.getLabel().equals(existingOptionLabel)) {
                    logger.warn("Option '{}) {}' is already known", newOption.getId(), newOption.getLabel());
                    return;
                }
                logger.warn("Overwriting the known option '{}) {}' with a new option '{}) {}'", newOption.getId(),
                        existingOptionLabel, newOption.getId(), newOption.getLabel());
            }
            instance.acknowledgeMenuOption(newOption.getId(), newOption.getLabel());
            logger.info("The numerical options of instance {} are now:\n{}", instance.getUuid(),
                    instance.getPendingOptions()
                            .entrySet()
                            .stream()
                            .map(entry -> entry.getKey() + "\t\t" + entry.getValue())
                            .collect(Collectors.joining("\n")));
        }

        private void onFileExistenceChanged(final boolean fileExists) {
            instance = terrariaInstanceRepository.getOne(instance.getId());
            @Nullable Boolean hasSession = null;
            final TerrariaInstanceEventEntity.TerrariaInstanceEventEntityBuilder eventBuilder =
                    TerrariaInstanceEventEntity
                    .builder()
                    .instance(instance);
            try {
                hasSession = tmuxService.hasSession(instance.getUuid().toString());
            } catch (final ProcessTimeoutException | ProcessFailureException e) {
                logger.error("Failed to check for the session of instance " + instance.getUuid(), e);
            } catch (final InterruptedException e) {
                logger.warn("Interrupted while checking for the session of instance " + instance.getUuid(), e);
                instance.setState(TerrariaInstanceState.BROKEN);
                instance.setError("TSW has been interrupted");
                instance = saveInstanceAndEvent(instance,
                        eventBuilder.type(TerrariaInstanceEventType.TSW_INTERRUPTED).build());
                Thread.currentThread().interrupt();
                return;
            }
            final TerrariaInstanceEventEntity event;
            instance.setNextOutputBytePosition(0L);
            if (hasSession != null && hasSession == fileExists) {
                instance.setState(fileExists ? TerrariaInstanceState.BOOTING_UP : TerrariaInstanceState.IDLE);
                instance.setLoadedMods(Collections.emptySet());
                event = eventBuilder.type(fileExists
                        ? TerrariaInstanceEventType.APPLICATION_START
                        : TerrariaInstanceEventType.APPLICATION_END).build();
            } else {
                final String error = String.format("The output file has been %s but %s",
                        fileExists ? "created" : "deleted", hasSession == null
                                ? "it's unknown if the instance is running or not"
                                : (hasSession ? "the instance is still running" : "the instance isn't running"));
                instance.setState(TerrariaInstanceState.BROKEN);
                instance.setError(error);
                event = eventBuilder.type(TerrariaInstanceEventType.ERROR).text(error).build();
            }
            instance = saveInstanceAndEvent(instance, event);
        }
    }
}
