package io.github.mewore.tsw.services.terraria;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.models.terraria.TerrariaWorldEntity;
import io.github.mewore.tsw.models.terraria.TerrariaWorldFileEntity;
import io.github.mewore.tsw.repositories.terraria.TerrariaWorldFileRepository;
import io.github.mewore.tsw.repositories.terraria.TerrariaWorldRepository;
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

    private static final int WLD_NAME_POSITION = 0x7F;

    private static final int LAST_CONTROL_CHARACTER_CODE = 31;

    private final Logger logger = LogManager.getLogger(getClass());

    private final @NonNull FileService fileService;

    private final @NonNull TerrariaWorldRepository terrariaWorldRepository;

    private final @NonNull TerrariaWorldFileRepository terrariaWorldFileRepository;

    List<TerrariaWorldInfo> getAllWorldInfo() {
        final Function<File, String> fileWithoutExtension = file -> file.getName()
                .substring(0, file.getName().lastIndexOf("."));

        logger.info("Checking for worlds in the following directory: {}", TERRARIA_WORLD_DIRECTORY);
        final List<String> worldFileNames = Arrays.stream(
                        fileService.listFilesWithExtensions(TERRARIA_WORLD_DIRECTORY.toFile(), "wld"))
                .map(fileWithoutExtension)
                .collect(Collectors.toUnmodifiableList());

        final List<TerrariaWorldInfo> worldInfoList = new ArrayList<>();
        for (final String fileName : worldFileNames) {
            final @Nullable TerrariaWorldInfo worldInfo = getWorldInfo(fileName, null);
            if (worldInfo != null) {
                worldInfoList.add(worldInfo);
            }
        }

        logger.info("Found {} worlds in the following directory: {}", worldInfoList.size(), TERRARIA_WORLD_DIRECTORY);
        return worldInfoList;
    }

    @Nullable TerrariaWorldInfo getWorldInfo(final TerrariaWorldEntity world) {
        return getWorldInfo(world.getDisplayName().replace(' ', '_'), world.getDisplayName());
    }

    private @Nullable TerrariaWorldInfo getWorldInfo(final String fileName, final @Nullable String worldName) {
        final File wldFile = fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve(fileName + "." + WLD_EXTENSION));
        final File twldFile = fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve(fileName + "." + TWLD_EXTENSION));
        if (!wldFile.exists() || !twldFile.exists()) {
            return null;
        }
        final Instant lastModified = Instant.ofEpochMilli(Math.max(wldFile.lastModified(), twldFile.lastModified()));
        final String displayName =
                worldName == null ? readWorldName(wldFile, fileName.replace('_', ' '), fileName.length()) : worldName;
        return new TerrariaWorldInfo(fileName, displayName, lastModified, wldFile, twldFile, fileService);
    }

    private String readWorldName(final File wldFile, final String nameFallback, final int expectedLength) {
        try (final InputStream stream = fileService.readFileInStream(wldFile)) {
            if (stream.skip(WLD_NAME_POSITION) < WLD_NAME_POSITION) {
                logger.warn("There are less than " + WLD_NAME_POSITION + " bytes in " + wldFile.getAbsolutePath() +
                        " so its name cannot be read");
                return nameFallback;
            }
            final int length = stream.read();
            if (length < 0) {
                logger.warn("There are exactly " + WLD_NAME_POSITION + " bytes in " + wldFile.getAbsolutePath() +
                        " so its name cannot be read");
                return nameFallback;
            }
            if (length != expectedLength) {
                logger.warn("The length of the name in " + wldFile.getAbsolutePath() + " is not " + expectedLength);
                return nameFallback;
            }
            final byte[] nameBytes = stream.readNBytes(length);
            if (nameBytes.length < length) {
                logger.warn("There are less than " + WLD_NAME_POSITION + 1 + length + " bytes in " +
                        wldFile.getAbsolutePath() + " so its name cannot be read");
                return nameFallback;
            }
            for (final byte nameByte : nameBytes) {
                if (nameByte <= LAST_CONTROL_CHARACTER_CODE) {
                    logger.warn("The name in " + wldFile.getAbsolutePath() +
                            " contains non-printable characters. Assuming it is invalid.");
                    return nameFallback;
                }
            }
            return new String(nameBytes);
        } catch (final IOException e) {
            logger.error("Failed to read " + wldFile.getName(), e);
            return nameFallback;
        }
    }

    void recreateWorld(final TerrariaWorldEntity world) throws IOException {
        final Optional<TerrariaWorldFileEntity> file = terrariaWorldFileRepository.findByWorld(world);
        if (file.isEmpty()) {
            logger.error("Cannot recreate world \"" + world.getDisplayName() + "\" because it has no file");
            world.setLastModified(null);
            terrariaWorldRepository.save(world);
            return;
        }
        fileService.unzip(new ByteArrayInputStream(file.get().getContent()), TERRARIA_WORLD_DIRECTORY.toFile());

        final @Nullable Instant lastModified = world.getLastModified();
        if (lastModified == null) {
            logger.warn("World \"" + world.getDisplayName() +
                    "\" has file data but not a lastModified timestamp. Leaving its files with the current timestamp.");
        } else {
            final String name = world.getFileName();
            fileService.setLastModified(TERRARIA_WORLD_DIRECTORY.resolve(name + "." + WLD_EXTENSION), lastModified);
            fileService.setLastModified(TERRARIA_WORLD_DIRECTORY.resolve(name + "." + TWLD_EXTENSION), lastModified);
        }
    }
}
