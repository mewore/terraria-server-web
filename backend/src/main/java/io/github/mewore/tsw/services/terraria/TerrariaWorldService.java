package io.github.mewore.tsw.services.terraria;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.terraria.TerrariaWorldEntity;
import io.github.mewore.tsw.models.terraria.TerrariaWorldFileEntity;
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

    private final @NonNull TerrariaWorldRepository terrariaWorldRepository;

    private final @NonNull TerrariaWorldFileRepository terrariaWorldFileRepository;

    @PostConstruct
    @Transactional
    void setUp() throws IOException {
        final HostEntity host = localHostService.getOrCreateHost();
        final List<TerrariaWorldInfo> newWorldInfoList = terrariaWorldFileService.getAllWorldInfo();

        final Map<String, TerrariaWorldEntity> currentWorlds = terrariaWorldRepository.findByHost(host)
                .stream()
                .collect(Collectors.toUnmodifiableMap(TerrariaWorldEntity::getName, Function.identity()));

        final List<TerrariaWorldEntity> worldsToSave = new ArrayList<>();

        for (final TerrariaWorldInfo worldInfo : newWorldInfoList) {
            final @Nullable TerrariaWorldEntity currentWorld = currentWorlds.get(worldInfo.getName());
            if (currentWorld == null) {
                final TerrariaWorldEntity newWorld = TerrariaWorldEntity.builder()
                        .name(worldInfo.getName())
                        .lastModified(worldInfo.getLastModified())
                        .file(worldInfo.readFile())
                        .host(host)
                        .build();
                worldsToSave.add(newWorld);
                continue;
            }
            if (currentWorld.getLastModified().equals(worldInfo.getLastModified())) {
                continue;
            }
            currentWorld.setLastModified(worldInfo.getLastModified());
            currentWorld.updateFile(worldInfo.readFile());
            currentWorld.setMods(null);
            worldsToSave.add(currentWorld);
        }

        logger.info("New worlds: {}",
                worldsToSave.stream().map(TerrariaWorldEntity::getName).collect(Collectors.joining(", ")));

        final Set<String> newWorldNames = newWorldInfoList.stream()
                .map(TerrariaWorldInfo::getName)
                .collect(Collectors.toUnmodifiableSet());

        terrariaWorldRepository.deleteAll(currentWorlds.values()
                .stream()
                .filter(world -> !newWorldNames.contains(world.getName()))
                .collect(Collectors.toList()));

        terrariaWorldRepository.saveAll(worldsToSave);
    }

    @Transactional
    public void updateWorld(final TerrariaWorldEntity world, final Set<String> newMods) {
        final @Nullable TerrariaWorldInfo worldInfo = terrariaWorldFileService.getWorldInfo(world.getName());
        if (worldInfo == null) {
            logger.warn("Could not get the info for world " + world.getName());
            return;
        }
        final TerrariaWorldFileEntity worldFile;
        try {
            worldFile = worldInfo.readFile();
        } catch (final IOException e) {
            logger.warn("Failed to get the data of world " + world.getName(), e);
            return;
        }
        if (world.getLastModified().equals(worldInfo.getLastModified())) {
            logger.info("World " + world.getName() + " is already up to date");
            return;
        }
        world.setLastModified(worldInfo.getLastModified());
        world.updateFile(worldFile);
        world.setMods(newMods);
        terrariaWorldFileRepository.save(world.getFile());
        terrariaWorldRepository.save(world);
    }
}
