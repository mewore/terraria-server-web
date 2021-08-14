package io.github.mewore.tsw.services.terraria;

import java.io.ByteArrayInputStream;
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

import io.github.mewore.tsw.models.terraria.TerrariaWorldEntity;
import io.github.mewore.tsw.services.util.FileService;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Service
public class TerrariaWorldFileService {

    private static final Path TERRARIA_WORLD_DIRECTORY = Path.of(System.getProperty("user.home"), ".local", "share",
            "Terraria", "ModLoader", "Worlds");

    private static final String WLD_EXTENSION = "wld";

    private static final String TWLD_EXTENSION = "twld";

    private final Logger logger = LogManager.getLogger(getClass());

    private final @NonNull FileService fileService;

    List<TerrariaWorldInfo> getAllWorldInfo() {
        final Function<File, String> fileWithoutExtension = file -> file.getName()
                .substring(0, file.getName().lastIndexOf("."));

        logger.info("Checking for worlds in the following directory: {}", TERRARIA_WORLD_DIRECTORY);
        final List<String> worldNames = Arrays.stream(
                        fileService.listFilesWithExtensions(TERRARIA_WORLD_DIRECTORY.toFile(), "wld"))
                .map(fileWithoutExtension)
                .collect(Collectors.toUnmodifiableList());

        final List<TerrariaWorldInfo> worldInfoList = new ArrayList<>();
        for (final String name : worldNames) {
            final @Nullable TerrariaWorldInfo worldInfo = getWorldInfo(name);
            if (worldInfo != null) {
                worldInfoList.add(worldInfo);
            }
        }

        logger.info("Found {} worlds in the following directory: {}", worldInfoList.size(), TERRARIA_WORLD_DIRECTORY);
        return worldInfoList;
    }

    @Nullable TerrariaWorldInfo getWorldInfo(final String name) {
        final File wldFile = fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve(name + "." + WLD_EXTENSION));
        final File twldFile = fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve(name + "." + TWLD_EXTENSION));
        if (!wldFile.exists() || !twldFile.exists()) {
            return null;
        }
        final Instant lastModified = Instant.ofEpochMilli(Math.max(wldFile.lastModified(), twldFile.lastModified()));
        return new TerrariaWorldInfo(name, lastModified, wldFile, twldFile, fileService);
    }

    void recreateWorld(final TerrariaWorldEntity world) throws IOException {
        fileService.unzip(new ByteArrayInputStream(world.getFile().getContent()), TERRARIA_WORLD_DIRECTORY.toFile());

        final String name = world.getName();
        final Instant lastModified = world.getLastModified();
        fileService.setLastModified(TERRARIA_WORLD_DIRECTORY.resolve(name + "." + WLD_EXTENSION), lastModified);
        fileService.setLastModified(TERRARIA_WORLD_DIRECTORY.resolve(name + "." + TWLD_EXTENSION), lastModified);
    }
}
