package io.github.mewore.tsw.services.terraria;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.terraria.TerrariaWorldEntity;
import io.github.mewore.tsw.repositories.terraria.TerrariaWorldRepository;
import io.github.mewore.tsw.services.LocalHostService;
import io.github.mewore.tsw.services.util.FileService;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Service
public class TerrariaWorldService {

    private static final File TERRARIA_WORLD_DIRECTORY =
            Path.of(System.getProperty("user.home"), ".local", "share", "Terraria", "ModLoader", "Worlds").toFile();

    private final Logger logger = LogManager.getLogger(getClass());

    private final @NonNull LocalHostService localHostService;

    private final @NonNull FileService fileService;

    private final @NonNull TerrariaWorldRepository terrariaWorldRepository;

    @PostConstruct
    void setUp() throws IOException {
        final HostEntity host = localHostService.getOrCreateHost();
        final Function<File, String> fileWithoutExtension =
                file -> file.getName().substring(0, file.getName().lastIndexOf("."));

        logger.info("Checking for worlds in the following directory: {}", TERRARIA_WORLD_DIRECTORY.getAbsolutePath());

        final Map<String, File> wldFiles = Arrays.stream(fileService.listFiles(TERRARIA_WORLD_DIRECTORY, "wld"))
                .collect(Collectors.toUnmodifiableMap(fileWithoutExtension, Function.identity()));
        final Map<String, File> twldFiles = Arrays.stream(fileService.listFiles(TERRARIA_WORLD_DIRECTORY, "twld"))
                .collect(Collectors.toUnmodifiableMap(fileWithoutExtension, Function.identity()));

        final List<TerrariaWorldEntity> newWorlds = new ArrayList<>();
        for (final Map.Entry<String, File> twldEntry : twldFiles.entrySet()) {
            final String worldName = twldEntry.getKey();
            final File wldFile = wldFiles.get(worldName);
            if (wldFile != null) {
                final File twldFile = twldEntry.getValue();
                final byte[] zipData = fileService.zip(wldFile, twldFile);
                final TerrariaWorldEntity world = TerrariaWorldEntity.builder()
                        .name(worldName)
                        .lastModified(Instant.ofEpochMilli(Math.max(wldFile.lastModified(), twldFile.lastModified())))
                        .data(zipData)
                        .host(host)
                        .build();
                newWorlds.add(world);
            }
        }

        logger.info("Found {} worlds in the following directory: {}", newWorlds.size(),
                TERRARIA_WORLD_DIRECTORY.getAbsolutePath());
        terrariaWorldRepository.setHostWorlds(host, newWorlds);
    }
}
