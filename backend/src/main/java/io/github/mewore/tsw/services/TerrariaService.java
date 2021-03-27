package io.github.mewore.tsw.services;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.config.security.AuthorityRoles;
import io.github.mewore.tsw.exceptions.IncorrectUrlException;
import io.github.mewore.tsw.exceptions.NotFoundException;
import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.github.GitHubRelease;
import io.github.mewore.tsw.models.github.GitHubReleaseAsset;
import io.github.mewore.tsw.models.terraria.TModLoaderVersionViewModel;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceCreationModel;
import io.github.mewore.tsw.models.terraria.TerrariaWorldEntity;
import io.github.mewore.tsw.repositories.terraria.TerrariaWorldRepository;
import io.github.mewore.tsw.services.util.FileService;
import io.github.mewore.tsw.services.util.HttpService;
import io.github.mewore.tsw.services.util.InputStreamSupplier;
import io.github.mewore.tsw.services.util.SystemService;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Service
public class TerrariaService {

    private static final Pattern TERRARIA_SERVER_URL_REGEX =
            Pattern.compile("^https?://(www\\.)?terraria\\.org/[^?]+/(terraria-server-(\\d{4}).zip)(\\?\\d+)?$");

    private static final String TERRARIA_SERVER_CACHE_DIR = "terraria-servers";

    private static final String T_MOD_LOADER_GITHUB_USER = "tModLoader";

    private static final String T_MOD_LOADER_GITHUB_REPO = "tModLoader";

    private static final File TERRARIA_WORLD_DIRECTORY =
            Path.of(System.getProperty("user.home"), ".local", "share", "Terraria", "ModLoader", "Worlds").toFile();

    private final @NonNull LocalHostService localHostService;

    private final @NonNull GithubService githubService;

    private final @NonNull HttpService httpService;

    private final @NonNull FileService fileService;

    private final @NonNull SystemService systemService;

    private final @NonNull TerrariaWorldRepository terrariaWorldRepository;

    @PostConstruct
    void setUp() throws IOException {
        final HostEntity host = localHostService.getHost();
        final Function<File, String> fileWithoutExtension =
                file -> file.getName().substring(0, file.getName().lastIndexOf("."));

        final Map<String, File> wldFiles = Arrays
                .stream(fileService.listFiles(TERRARIA_WORLD_DIRECTORY, "wld"))
                .collect(Collectors.toUnmodifiableMap(fileWithoutExtension, Function.identity()));
        final Map<String, File> twldFiles = Arrays
                .stream(fileService.listFiles(TERRARIA_WORLD_DIRECTORY, "twld"))
                .collect(Collectors.toUnmodifiableMap(fileWithoutExtension, Function.identity()));

        final List<TerrariaWorldEntity> newWorlds = new ArrayList<>();
        for (final Map.Entry<String, File> twldEntry : twldFiles.entrySet()) {
            final String worldName = twldEntry.getKey();
            final File wldFile = wldFiles.get(worldName);
            if (wldFile != null) {
                final File twldFile = twldEntry.getValue();
                final byte[] zipData = fileService.zip(wldFile, twldFile);
                final TerrariaWorldEntity world = new TerrariaWorldEntity(null, worldName,
                        Instant.ofEpochMilli(Math.max(wldFile.lastModified(), twldFile.lastModified())), zipData, host);
                newWorlds.add(world);
            }
        }

        terrariaWorldRepository.setHostWorlds(host, newWorlds);
    }

    public List<TModLoaderVersionViewModel> fetchTModLoaderVersions() throws IOException {
        return githubService
                .getReleases(T_MOD_LOADER_GITHUB_USER, T_MOD_LOADER_GITHUB_REPO)
                .stream()
                .map(release -> new TModLoaderVersionViewModel(release.getId(), release.getName()))
                .collect(Collectors.toUnmodifiableList());
    }

    @Secured({AuthorityRoles.MANAGE_HOSTS})
    public void createTerrariaInstance(final TerrariaInstanceCreationModel creationModel)
            throws IOException, NotFoundException, IncorrectUrlException {

        // Validation
        final String serverZipUrl = creationModel.getTerrariaServerArchiveUrl();
        final Matcher serverZipUrlMatcher = TERRARIA_SERVER_URL_REGEX.matcher(serverZipUrl);
        if (!serverZipUrlMatcher.find()) {
            throw new IncorrectUrlException("The URL " + serverZipUrl + " does not match the regular expression " +
                    TERRARIA_SERVER_URL_REGEX.toString());
        }

        // Preparation
        final String serverZipName = serverZipUrlMatcher.group(2);
        final String serverRawVersion = serverZipUrlMatcher.group(3);
        final Path serverZipCacheLocation = Path.of(TERRARIA_SERVER_CACHE_DIR, serverZipName);
        final InputStreamSupplier serverFetcher =
                () -> httpService.requestAsStream(new URL(creationModel.getTerrariaServerArchiveUrl()));

        final @NonNull String tModLoaderFileOsString;
        final @NonNull String serverZipSubdirectory;
        switch (systemService.getOs()) {
            case WINDOWS -> {
                serverZipSubdirectory = "Windows";
                tModLoaderFileOsString = "Windows";
            }
            case MAC -> {
                serverZipSubdirectory = "Mac";
                tModLoaderFileOsString = "Mac";
            }
            case LINUX -> {
                serverZipSubdirectory = "Linux";
                tModLoaderFileOsString = "Linux";
            }
            default -> throw new UnsupportedOperationException(
                    "Cannot run the Terraria server on the current operating system: " + systemService.getOs());
        }
        final GitHubReleaseAsset tModLoaderAsset =
                getTModLoaderAsset(creationModel.getModLoaderReleaseId(), tModLoaderFileOsString);

        final UUID instanceUuid = UUID.randomUUID();
        final HostEntity host = localHostService.getHost();

        // Creation
        final File instanceDirectory =
                fileService.reserveDirectory(host.getTerrariaInstanceDirectory().resolve(instanceUuid.toString()));
        try (final InputStream serverZipStream = fileService.cache(serverZipCacheLocation, serverFetcher, null)) {
            fileService.unzip(serverZipStream, instanceDirectory,
                    Path.of(serverRawVersion, serverZipSubdirectory).toString());
        }

        try (final InputStream tModLoaderZipStream = githubService.fetchAsset(tModLoaderAsset)) {
            fileService.unzip(tModLoaderZipStream, instanceDirectory);
        }
    }

    private @NonNull GitHubReleaseAsset getTModLoaderAsset(final long releaseId, final @NonNull String osString)
            throws NotFoundException, IOException {

        final GitHubRelease tModLoaderRelease =
                githubService.getRelease(T_MOD_LOADER_GITHUB_USER, T_MOD_LOADER_GITHUB_REPO, releaseId);

        return tModLoaderRelease
                .getAssets()
                .stream()
                .filter(asset -> asset.getBrowserDownloadUrl() != null &&
                        asset.getBrowserDownloadUrl().endsWith(".zip") &&
                        asset.getBrowserDownloadUrl().contains("." + osString + "."))
                .findAny()
                .orElseThrow(() -> new NotFoundException(
                        "Could not find a " + osString + " archive of the TModLoader release " +
                                tModLoaderRelease.getId()));
    }
}
