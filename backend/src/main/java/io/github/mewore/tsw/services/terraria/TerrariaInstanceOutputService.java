package io.github.mewore.tsw.services.terraria;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventType;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;
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

    private final TerrariaInstanceService terrariaInstanceService;

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

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    private static class WorldMenuOption {

        private static final Pattern WORLD_MENU_OPTION_PATTERN = Pattern.compile("^(\\d+)\t+([^\t]+)$");

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

        List<TerrariaInstanceEventEntity> events = new ArrayList<>();

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
            if (character != '\n') {
                outputTextBuffer.append(character);
                return;
            }
            final String line = outputTextBuffer.toString();
            outputTextBuffer = new StringBuilder();

            if (instance.getState() != TerrariaInstanceState.RUNNING && applyLineToInstance(line)) {
                events.add(makeEvent(TerrariaInstanceEventType.IMPORTANT_OUTPUT, line));
                events.add(makeEvent(TerrariaInstanceEventType.OUTPUT, "\n"));
            } else if (instance.getState() == TerrariaInstanceState.PASSWORD_PROMPT && line.endsWith("%")) {
                events.add(makeEvent(TerrariaInstanceEventType.DETAILED_OUTPUT, line + "\n"));
            } else {
                events.add(makeEvent(TerrariaInstanceEventType.OUTPUT, line + "\n"));
            }
            instance.setNextOutputBytePosition(position + 1);
        }

        @Override
        public void onReadFinished(final long endPosition) {
            final String remainingText = outputTextBuffer.toString();
            if (events.isEmpty() && remainingText.isEmpty()) {
                return;
            }

            logger.info("Reached the end of the output file. Text:\n<{}>",
                    events.stream().map(TerrariaInstanceEventEntity::getContent).collect(Collectors.joining()) +
                            remainingText);

            outputTextBuffer = new StringBuilder();
            if (!remainingText.isEmpty()) {
                if (instance.getState() == TerrariaInstanceState.RUNNING) {
                    events.add(makeEvent(TerrariaInstanceEventType.OUTPUT, remainingText));
                    instance.setNextOutputBytePosition(endPosition);
                } else if (applyLineToInstanceState(remainingText)) {
                    events.add(makeEvent(TerrariaInstanceEventType.IMPORTANT_OUTPUT, remainingText));
                    instance.setNextOutputBytePosition(endPosition);
                } else {
                    outputTextBuffer = new StringBuilder(remainingText);
                }
            }
            instance = terrariaInstanceService.saveInstanceAndEvents(instance, combineEvents());
            events = new ArrayList<>();
        }

        @Override
        public void onFileDeleted() {
            onFileExistenceChanged(false);
        }

        private TerrariaInstanceEventEntity makeEvent(final TerrariaInstanceEventType type, final String text) {
            final String fixedText =
                    instance.getState() == TerrariaInstanceState.RUNNING && type == TerrariaInstanceEventType.OUTPUT
                            ? text.replaceAll(
                            "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{1,6} is connecting\\.\\.\\.",
                            "[REDACTED IP] is connecting...")
                            : text;
            return TerrariaInstanceEventEntity.builder().type(type).content(fixedText).instance(instance).build();
        }

        private List<TerrariaInstanceEventEntity> combineEvents() {
            if (events.isEmpty()) {
                return Collections.emptyList();
            }
            final List<TerrariaInstanceEventEntity> result = new ArrayList<>();
            StringBuilder combinedText = new StringBuilder(events.get(0).getContent());
            for (int i = 1; i <= events.size(); i++) {
                if (i >= events.size() || events.get(i).getType() != events.get(i - 1).getType()) {
                    result.add(makeEvent(events.get(i - 1).getType(), combinedText.toString()));
                    combinedText = new StringBuilder();
                }
                if (i < events.size()) {
                    combinedText.append(events.get(i).getContent());
                }
            }
            return result;
        }

        /**
         * Modify the current instance based on a line.
         *
         * @param line The line to apply to the instance.
         */
        private boolean applyLineToInstance(final String line) {
            return applyLineToInstanceState(line) || applyLineToInstanceMods(line) || applyLineToInstanceOptions(line);
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

        private boolean applyLineToInstanceOptions(final String line) {
            final @Nullable WorldMenuOption newOption = WorldMenuOption.fromLine(line);
            if (newOption == null) {
                return false;
            }
            final @Nullable String existingOptionLabel = instance.getPendingOptions().get(newOption.getId());
            if (existingOptionLabel != null) {
                if (newOption.getLabel().equals(existingOptionLabel)) {
                    logger.warn("Option '{}) {}' is already known", newOption.getId(), newOption.getLabel());
                    return true;
                }
                logger.warn("Overwriting the known option '{}) {}' with a new option '{}) {}'", newOption.getId(),
                        existingOptionLabel, newOption.getId(), newOption.getLabel());
            }
            instance.acknowledgeMenuOption(newOption.getId(), newOption.getLabel());
            logger.debug("The numerical options of instance {} are now:\n{}", instance.getUuid(),
                    instance.getPendingOptions()
                            .entrySet()
                            .stream()
                            .map(entry -> entry.getKey() + "\t\t" + entry.getValue())
                            .collect(Collectors.joining("\n")));
            return true;
        }

        private void onFileExistenceChanged(final boolean fileExists) {
            instance = terrariaInstanceRepository.getOne(instance.getId());
            @Nullable Boolean hasSession = null;
            final TerrariaInstanceEventEntity.TerrariaInstanceEventEntityBuilder eventBuilder =
                    TerrariaInstanceEventEntity.builder()
                    .instance(instance);
            try {
                hasSession = tmuxService.hasSession(instance.getUuid().toString());
            } catch (final ProcessTimeoutException | ProcessFailureException e) {
                logger.error("Failed to check for the session of instance " + instance.getUuid(), e);
            } catch (final InterruptedException e) {
                logger.warn("Interrupted while checking for the session of instance " + instance.getUuid(), e);
                instance.setState(TerrariaInstanceState.BROKEN);
                instance.setError("TSW has been interrupted");
                instance = terrariaInstanceService.saveInstanceAndEvent(instance,
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
                event = eventBuilder.type(TerrariaInstanceEventType.ERROR).content(error).build();
            }
            instance = terrariaInstanceService.saveInstanceAndEvent(instance, event);
        }
    }
}
