package io.github.mewore.tsw.services.terraria;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.file.FileDataEntity;
import io.github.mewore.tsw.models.terraria.TerrariaWorldEntity;
import io.github.mewore.tsw.repositories.file.FileDataRepository;
import io.github.mewore.tsw.repositories.terraria.TerrariaWorldRepository;
import io.github.mewore.tsw.services.LocalHostService;
import io.github.mewore.tsw.services.util.FileService;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Service
public class TerrariaWorldService {

    private static final Path TERRARIA_WORLD_DIRECTORY = Path.of(System.getProperty("user.home"), ".local", "share",
            "Terraria", "ModLoader", "Worlds");

    private static final String WLD_EXTENSION = "wld";

    private static final String TWLD_EXTENSION = "twld";

    private final Logger logger = LogManager.getLogger(getClass());

    private final @NonNull LocalHostService localHostService;

    private final @NonNull FileService fileService;

    private final @NonNull TerrariaWorldRepository terrariaWorldRepository;

    private final @NonNull FileDataRepository fileDataRepository;

    @PostConstruct
    void setUp() throws IOException {
        final HostEntity host = localHostService.getOrCreateHost();
        final Function<File, String> fileWithoutExtension = file -> file.getName()
                .substring(0, file.getName().lastIndexOf("."));

        logger.info("Checking for worlds in the following directory: {}", TERRARIA_WORLD_DIRECTORY);
        final List<String> worldNames = Arrays.stream(
                fileService.listFilesWithExtensions(TERRARIA_WORLD_DIRECTORY.toFile(), "wld"))
                .map(fileWithoutExtension)
                .collect(Collectors.toUnmodifiableList());

        final List<TerrariaWorldEntity> newWorlds = new ArrayList<>();
        for (final String name : worldNames) {
            final @Nullable TerrariaWorldEntity world = readWorld(name, host);
            if (world != null) {
                newWorlds.add(world);
            }
        }
        fileDataRepository.saveAll(
                newWorlds.stream().map(TerrariaWorldEntity::getData).collect(Collectors.toUnmodifiableSet()));

        logger.info("Found {} worlds in the following directory: {}", newWorlds.size(), TERRARIA_WORLD_DIRECTORY);
        terrariaWorldRepository.setHostWorlds(host, newWorlds);
    }

    public @Nullable TerrariaWorldEntity readWorld(final TerrariaWorldEntity world) {
        try {
            return readWorld(world.getName(), world.getHost());
        } catch (final IOException e) {
            logger.error("Failed to read world " + world.getName(), e);
            return null;
        }
    }

    private @Nullable TerrariaWorldEntity readWorld(final String name, final HostEntity host) throws IOException {
        final File wldFile = fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve(name + "." + WLD_EXTENSION));
        final File twldFile = fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve(name + "." + TWLD_EXTENSION));
        if (!wldFile.exists() || !twldFile.exists()) {
            return null;
        }
        final byte[] zipData = fileService.zip(wldFile, twldFile);
        return TerrariaWorldEntity.builder()
                .name(name)
                .lastModified(Instant.ofEpochMilli(Math.max(wldFile.lastModified(), twldFile.lastModified())))
                .data(FileDataEntity.builder().name(name + ".zip").content(zipData).build())
                .host(host)
                .build();
    }
}
