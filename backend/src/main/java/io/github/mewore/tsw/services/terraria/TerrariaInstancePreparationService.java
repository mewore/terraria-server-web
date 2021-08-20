package io.github.mewore.tsw.services.terraria;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import io.github.mewore.tsw.config.security.AuthorityRoles;
import io.github.mewore.tsw.exceptions.InvalidInstanceException;
import io.github.mewore.tsw.exceptions.NotFoundException;
import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.github.GitHubRelease;
import io.github.mewore.tsw.models.github.GitHubReleaseAsset;
import io.github.mewore.tsw.models.terraria.TModLoaderVersionViewModel;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceAction;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceDefinitionModel;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;
import io.github.mewore.tsw.repositories.HostRepository;
import io.github.mewore.tsw.services.GithubService;
import io.github.mewore.tsw.services.util.FileService;
import io.github.mewore.tsw.services.util.HttpService;
import io.github.mewore.tsw.services.util.InputStreamSupplier;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Service
public class TerrariaInstancePreparationService {

    private static final String TERRARIA_SERVER_CACHE_DIR = "terraria-servers";

    private static final String T_MOD_LOADER_GITHUB_USER = "tModLoader";

    private static final String T_MOD_LOADER_GITHUB_REPO = "tModLoader";

    private static final Pattern T_MOD_LOADER_SERVER_NAME_PATTERN = Pattern.compile("^tModLoaderServer.*");

    private final Logger logger = LogManager.getLogger(getClass());

    private final GithubService githubService;

    private final HttpService httpService;

    private final FileService fileService;

    private final HostRepository hostRepository;

    private final TerrariaInstanceService terrariaInstanceService;

    private static @NonNull String getTModLoaderFileOsString(final HostEntity host) throws InvalidInstanceException {
        switch (host.getOs()) {
            case WINDOWS: {
                return "Windows";
            }
            case MAC: {
                return "Mac";
            }
            case LINUX: {
                return "Linux";
            }
            default: {
                throw new InvalidInstanceException(
                        String.format("TModLoader is not supported for the [%s] operating system", host.getOs()));
            }
        }
    }

    private static @NonNull String getServerZipSubdirectory(final HostEntity host) throws InvalidInstanceException {
        switch (host.getOs()) {
            case WINDOWS: {
                return "Windows";
            }
            case MAC: {
                return "Mac";
            }
            case LINUX: {
                return "Linux";
            }
            default: {
                throw new InvalidInstanceException(
                        String.format("Cannot run the Terraria server on the [%s] operating system", host.getOs()));
            }
        }
    }

    public List<TModLoaderVersionViewModel> fetchTModLoaderVersions() throws IOException {
        return githubService.getReleases(T_MOD_LOADER_GITHUB_USER, T_MOD_LOADER_GITHUB_REPO)
                .stream()
                .map(release -> new TModLoaderVersionViewModel(release.getId(), release.getName()))
                .collect(Collectors.toUnmodifiableList());
    }

    @Secured({AuthorityRoles.MANAGE_HOSTS})
    public TerrariaInstanceEntity defineTerrariaInstance(final long hostId,
            final TerrariaInstanceDefinitionModel creationModel) throws NotFoundException {

        final UUID instanceUuid = UUID.randomUUID();
        final HostEntity host = hostRepository.findById(hostId)
                .orElseThrow(() -> new NotFoundException("There is no host with ID " + hostId));
        final Path instanceDirectory = host.getTerrariaInstanceDirectory().resolve(instanceUuid.toString());

        final TerrariaInstanceEntity newTerrariaInstance = TerrariaInstanceEntity.builder()
                .uuid(instanceUuid)
                .location(instanceDirectory.toAbsolutePath().toString())
                .name(creationModel.getInstanceName())
                .terrariaServerUrl(creationModel.getTerrariaServerArchiveUrl())
                .modLoaderReleaseId(creationModel.getModLoaderReleaseId())
                .state(TerrariaInstanceState.DEFINED)
                .pendingAction(TerrariaInstanceAction.SET_UP)
                .host(host)
                .build();

        final TerrariaInstanceEntity result = terrariaInstanceService.saveInstance(newTerrariaInstance);

        logger.info("Defined a Terraria instance at {}", instanceDirectory);
        return result;
    }

    @Synchronized
    TerrariaInstanceEntity setUpInstance(TerrariaInstanceEntity instance) throws IOException, InvalidInstanceException {

        // Validation
        final TerrariaServerInfo serverInfo = TerrariaServerInfo.fromInstance(instance);

        // Preparation
        final @NonNull String tModLoaderFileOsString = getTModLoaderFileOsString(instance.getHost());
        final @NonNull String serverZipSubdirectory = getServerZipSubdirectory(instance.getHost());
        final GitHubRelease tModLoaderRelease;
        final GitHubReleaseAsset tModLoaderAsset;
        try {
            tModLoaderRelease = githubService.getRelease(T_MOD_LOADER_GITHUB_USER, T_MOD_LOADER_GITHUB_REPO,
                    instance.getModLoaderReleaseId());
            tModLoaderAsset = getTModLoaderAsset(tModLoaderRelease, tModLoaderFileOsString);
        } catch (final NotFoundException e) {
            throw new InvalidInstanceException(e.getMessage(), e);
        }

        final Path serverZipCacheLocation = Path.of(TERRARIA_SERVER_CACHE_DIR, serverInfo.getZipName());
        if (!fileService.hasFileInCache(serverZipCacheLocation)) {
            try {
                httpService.checkUrl(serverInfo.getUrl());
            } catch (final HttpClientErrorException e) {
                throw new InvalidInstanceException(
                        String.format("The response at URL %s is HTTP code %d: %s", serverInfo.getUrl(),
                                e.getRawStatusCode(), e.getStatusText()), e);
            }
        }

        instance = markInstanceAsValid(instance, serverInfo, tModLoaderRelease, tModLoaderAsset);

        // Creation
        final File instanceDirectory = fileService.reserveDirectory(instance.getLocation());
        final InputStreamSupplier serverFetcher = () -> httpService.requestAsStream(serverInfo.getUrl());
        try (final InputStream serverZipStream = fileService.cache(serverZipCacheLocation, serverFetcher, null)) {
            fileService.unzip(serverZipStream, instanceDirectory,
                    Path.of(serverInfo.getRawVersion(), serverZipSubdirectory).toString());
        }

        try (final InputStream tModLoaderZipStream = githubService.fetchAsset(tModLoaderAsset)) {
            fileService.unzip(tModLoaderZipStream, instanceDirectory);
        }

        if (!fileService.makeFilesInDirectoryExecutable(instanceDirectory, T_MOD_LOADER_SERVER_NAME_PATTERN)) {
            throw new RuntimeException(
                    "Failed to find any files, the name of which matches /" + T_MOD_LOADER_SERVER_NAME_PATTERN + "/");
        }

        instance.setState(TerrariaInstanceState.IDLE);
        instance = terrariaInstanceService.saveInstance(instance);

        logger.info("Created a Terraria instance at {}", instanceDirectory.getAbsolutePath());
        return instance;
    }

    private TerrariaInstanceEntity markInstanceAsValid(final TerrariaInstanceEntity instance,
            final TerrariaServerInfo serverInfo,
            final GitHubRelease tModLoaderRelease,
            final GitHubReleaseAsset tModLoaderAsset) throws NullPointerException {

        instance.setTerrariaVersion(serverInfo.getFormattedVersion());
        instance.setModLoaderVersion(tModLoaderRelease.getName().substring(1));
        instance.setModLoaderArchiveUrl(tModLoaderAsset.getBrowserDownloadUrl());
        instance.setModLoaderReleaseUrl(tModLoaderRelease.getHtmlUrl());
        instance.setState(TerrariaInstanceState.VALID);
        return terrariaInstanceService.saveInstance(instance);
    }

    private @NonNull GitHubReleaseAsset getTModLoaderAsset(final @NonNull GitHubRelease tModLoaderRelease,
            final @NonNull String osString) throws NotFoundException {
        return tModLoaderRelease.getAssets()
                .stream()
                .filter(asset -> {
                    final String downloadUrl = asset.getBrowserDownloadUrl();
                    return downloadUrl != null && downloadUrl.endsWith(".zip") &&
                            downloadUrl.contains("." + osString + ".");
                })
                .findAny()
                .orElseThrow(() -> new NotFoundException(
                        "Could not find a " + osString + " archive of the TModLoader release " +
                                tModLoaderRelease.getId()));
    }
}
