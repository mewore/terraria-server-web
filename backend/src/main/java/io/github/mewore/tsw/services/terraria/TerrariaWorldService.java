package io.github.mewore.tsw.services.terraria;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.events.TerrariaWorldDeletionEvent;
import io.github.mewore.tsw.exceptions.InvalidRequestException;
import io.github.mewore.tsw.exceptions.NotFoundException;
import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.terraria.world.TerrariaWorldEntity;
import io.github.mewore.tsw.models.terraria.world.TerrariaWorldFileEntity;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceRepository;
import io.github.mewore.tsw.repositories.terraria.TerrariaWorldFileRepository;
import io.github.mewore.tsw.repositories.terraria.TerrariaWorldRepository;
import io.github.mewore.tsw.services.LocalHostService;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Service
public class TerrariaWorldService {

    private final Logger logger = LogManager.getLogger(getClass());

    private final @NonNull LocalHostService localHostService;

    private final @NonNull TerrariaWorldFileService terrariaWorldFileService;

    private final @NonNull TerrariaInstanceRepository terrariaInstanceRepository;

    private final @NonNull TerrariaWorldRepository terrariaWorldRepository;

    private final @NonNull TerrariaWorldFileRepository terrariaWorldFileRepository;

    private final @NonNull TerrariaWorldDbNotificationService terrariaWorldDbNotificationService;

    private final @NonNull ApplicationEventPublisher applicationEventPublisher;

    @PostConstruct
    @Transactional
    void setUp() throws IOException {
        final HostEntity host = localHostService.getOrCreateHost();
        final List<TerrariaWorldInfo> newWorldInfoList = terrariaWorldFileService.getAllWorldInfo();

        final List<TerrariaWorldEntity> currentWorlds = terrariaWorldRepository.findByHost(host);
        final Map<String, TerrariaWorldEntity> currentWorldsByName = currentWorlds.stream()
                .collect(Collectors.toUnmodifiableMap(TerrariaWorldEntity::getFileName, Function.identity()));

        final List<TerrariaWorldFileEntity> filesToSave = new ArrayList<>();

        for (final TerrariaWorldInfo worldInfo : newWorldInfoList) {
            final @Nullable TerrariaWorldEntity currentWorld = currentWorldsByName.get(worldInfo.getFileName());
            if (currentWorld != null) {
                updateWorld(currentWorld, null, worldInfo);
                continue;
            }
            final TerrariaWorldEntity newWorld = TerrariaWorldEntity.builder()
                    .fileName(worldInfo.getFileName())
                    .displayName(worldInfo.getDisplayName())
                    .lastModified(worldInfo.getLastModified())
                    .host(host)
                    .build();
            filesToSave.add(worldInfo.readFile(newWorld));
        }

        final Set<String> newWorldNames = newWorldInfoList.stream()
                .map(TerrariaWorldInfo::getFileName)
                .collect(Collectors.toUnmodifiableSet());

        for (final TerrariaWorldEntity world : currentWorlds) {
            if (!newWorldNames.contains(world.getFileName())) {
                terrariaWorldFileService.recreateWorld(world);
            }
        }

        final Set<String> allWorldNames = new HashSet<>(newWorldNames);
        allWorldNames.addAll(currentWorldsByName.keySet());
        logger.info("New worlds: {}", String.join(", ", allWorldNames));

        terrariaWorldFileRepository.saveAll(filesToSave);
    }

    public TerrariaWorldFileEntity getWorldData(final long worldId) throws NotFoundException {
        return terrariaWorldFileRepository.findById(worldId)
                .orElseThrow(() -> new NotFoundException("There is no file data for the world with ID " + worldId));
    }

    @Transactional
    public TerrariaWorldEntity updateWorld(final TerrariaWorldEntity world, final Set<String> newMods) {
        final @Nullable TerrariaWorldInfo worldInfo = terrariaWorldFileService.getWorldInfo(world);
        if (worldInfo == null) {
            logger.warn("Could not get the info for world \"" + world.getDisplayName() + "\"");
            return world;
        }
        try {
            return updateWorld(world, newMods, worldInfo);
        } catch (final IOException e) {
            logger.warn("Failed to get the data of world \"" + world.getDisplayName() + "\"", e);
            return world;
        }
    }

    @Transactional
    private TerrariaWorldEntity updateWorld(TerrariaWorldEntity world,
            final @Nullable Set<String> newMods,
            final TerrariaWorldInfo worldInfo) throws IOException {

        logger.info("Updating world \"" + world.getDisplayName() + "\"");
        if (worldInfo.getLastModified().equals(world.getLastModified())) {
            logger.info("World \"" + world.getDisplayName() + "\" is already up to date");
            return world;
        }

        world.setDisplayName(worldInfo.getDisplayName());
        world.setLastModified(worldInfo.getLastModified());
        world.setMods(newMods);
        world = terrariaWorldRepository.save(world);

        final TerrariaWorldFileEntity worldFile = worldInfo.readFile(world);
        final Optional<TerrariaWorldFileEntity> file = terrariaWorldFileRepository.findByWorld(world);
        if (file.isPresent()) {
            terrariaWorldFileRepository.save(file.get().update(worldFile, world));
        } else {
            terrariaWorldFileRepository.save(worldFile);
        }

        return world;
    }

    @Transactional
    public void deleteWorld(final long worldId) throws NotFoundException, InvalidRequestException {
        final TerrariaWorldEntity world = terrariaWorldRepository.findById(worldId)
                .orElseThrow(() -> new NotFoundException("There is no world with ID " + worldId));
        if (terrariaInstanceRepository.existsByWorld(world)) {
            throw new InvalidRequestException("World \"" + world.getDisplayName() + "\" with ID " + worldId +
                    " is used by one or more instances. Stop the instances first.");
        }
        final long deletedFileCount = terrariaWorldFileRepository.deleteByWorld(world);
        logger.info("Deleted {} file record(s) of world \"{}\"", deletedFileCount, world.getDisplayName());
        terrariaWorldRepository.delete(world);
        applicationEventPublisher.publishEvent(new TerrariaWorldDeletionEvent(world));
        terrariaWorldDbNotificationService.worldDeleted(world);
    }
}
