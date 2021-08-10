package io.github.mewore.tsw.services.terraria;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.events.TerrariaInstanceUpdatedEvent;
import io.github.mewore.tsw.exceptions.InvalidRequestException;
import io.github.mewore.tsw.exceptions.NotFoundException;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceAction;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceRunConfiguration;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceUpdateModel;
import io.github.mewore.tsw.models.terraria.TerrariaWorldEntity;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceEventRepository;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceRepository;
import io.github.mewore.tsw.repositories.terraria.TerrariaWorldRepository;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Service
public class TerrariaInstanceService {

    private final Logger logger = LogManager.getLogger(getClass());

    private final TerrariaInstanceRepository terrariaInstanceRepository;

    private final TerrariaInstanceEventRepository terrariaInstanceEventRepository;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final TerrariaWorldRepository terrariaWorldRepository;

    private final TerrariaMessageService terrariaMessageService;

    public TerrariaInstanceEntity getInstance(final long instanceId) throws NotFoundException {
        return terrariaInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new NotFoundException("Could not find a Terraria instance with ID " + instanceId));
    }

    public TerrariaInstanceEntity saveInstance(final TerrariaInstanceEntity instance) {
        final boolean isNew = instance.getId() == null;
        final TerrariaInstanceEntity result = terrariaInstanceRepository.save(instance);
        applicationEventPublisher.publishEvent(new TerrariaInstanceUpdatedEvent(result));

        if (isNew) {
            terrariaMessageService.broadcastInstanceCreation(result);
        } else {
            terrariaMessageService.broadcastInstanceChange(result);
        }
        return result;
    }

    @Transactional
    public TerrariaInstanceEntity saveInstanceAndEvents(final TerrariaInstanceEntity instance,
            final List<TerrariaInstanceEventEntity> events) {
        final List<TerrariaInstanceEventEntity> savedEvents = terrariaInstanceEventRepository.saveAll(events);
        for (final TerrariaInstanceEventEntity event : savedEvents) {
            terrariaMessageService.broadcastInstanceEvent(event);
        }
        return saveInstance(instance);
    }

    @Transactional
    public TerrariaInstanceEntity saveInstanceAndEvent(final TerrariaInstanceEntity instance,
            final TerrariaInstanceEventEntity event) {
        final TerrariaInstanceEventEntity savedEvent = terrariaInstanceEventRepository.save(event);
        terrariaMessageService.broadcastInstanceEvent(savedEvent);
        return saveInstance(instance);
    }

    @Transactional
    public void saveEvent(final TerrariaInstanceEventEntity event) {
        final TerrariaInstanceEventEntity savedEvent = terrariaInstanceEventRepository.save(event);
        terrariaMessageService.broadcastInstanceEvent(savedEvent);
    }

    public void ensureInstanceHasNoOutputFile(final TerrariaInstanceEntity instance) {
        if (instance.getOutputFile().exists() && !instance.getOutputFile().delete()) {
            throw new IllegalStateException("Failed to delete file " + instance.getOutputFile().getAbsolutePath());
        }
    }

    @Transactional
    public TerrariaInstanceEntity updateInstance(final long instanceId, final TerrariaInstanceUpdateModel model)
            throws NotFoundException, InvalidRequestException {
        final TerrariaInstanceEntity instance = getInstance(instanceId);
        @Nullable TerrariaInstanceAction actionToApply = null;

        final @Nullable String newName = model.getNewName();
        if (newName != null) {
            instance.setName(newName);
        }

        final @Nullable Set<String> newMods = model.getNewMods();
        if (newMods != null) {
            instance.setModsToEnable(newMods);
            try {
                getDesiredModOption(instance);
            } catch (final IllegalArgumentException e) {
                final @Nullable String message = e.getMessage();
                throw new InvalidRequestException(
                        message == null ? "Failed to map the requested enabled mods to the list of options" : message);
            }
            actionToApply = TerrariaInstanceAction.SET_LOADED_MODS;
        }

        final @Nullable TerrariaInstanceRunConfiguration runConfiguration = model.getRunConfiguration();
        if (runConfiguration != null) {
            final TerrariaWorldEntity world = terrariaWorldRepository.findById(runConfiguration.getWorldId())
                    .orElseThrow(() -> new InvalidRequestException(
                            "There is no world with ID " + runConfiguration.getWorldId()));
            instance.setMaxPlayers(runConfiguration.getMaxPlayers());
            instance.setPort(runConfiguration.getPort());
            instance.setAutomaticallyForwardPort(runConfiguration.isAutomaticallyForwardPort());
            instance.setPassword(runConfiguration.getPassword());
            instance.setWorld(world);
            actionToApply = TerrariaInstanceAction.RUN_SERVER;
        }

        if (model.getNewAction() != null) {
            actionToApply = model.getNewAction();
        }
        if (actionToApply != null) {
            if (instance.getPendingAction() != null) {
                throw new InvalidRequestException(
                        "Cannot apply an action to an instance that already has a pending action");
            }
            if (!actionToApply.isApplicableTo(instance.getState())) {
                throw new InvalidRequestException(
                        "Cannot apply action " + actionToApply + " to an instance with the state " +
                                instance.getState());
            }
            instance.setPendingAction(actionToApply);
        }

        return saveInstance(instance);
    }

    @Transactional
    public void deleteInstance(final TerrariaInstanceEntity instance) {
        logger.info("Clearing instance {} events...", instance.getUuid());
        final long deletedEventCount = terrariaInstanceEventRepository.deleteByInstance(instance);
        logger.info("Deleted {} events of instance {}", deletedEventCount, instance.getUuid());

        terrariaInstanceRepository.delete(instance);
        logger.info("Done deleting instance {}", instance.getUuid());
        terrariaMessageService.broadcastInstanceDeletion(instance);
    }

    public @Nullable Integer getDesiredModOption(final TerrariaInstanceEntity instance) {
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
