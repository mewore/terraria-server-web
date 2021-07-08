package io.github.mewore.tsw.services.terraria;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceRepository;
import io.github.mewore.tsw.services.GithubService;
import io.github.mewore.tsw.services.util.FileService;
import io.github.mewore.tsw.services.util.HttpService;
import io.github.mewore.tsw.services.util.InputStreamSupplier;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Service
public class TerrariaInstanceService {

    private static final Pattern TERRARIA_SERVER_URL_REGEX =
            Pattern.compile("^https?://(www\\.)?terraria\\.org/[^?]+/(terraria-server-(\\d+).zip)(\\?\\d+)?$");

    private static final String TERRARIA_SERVER_CACHE_DIR = "terraria-servers";

    private static final String T_MOD_LOADER_GITHUB_USER = "tModLoader";

    private static final String T_MOD_LOADER_GITHUB_REPO = "tModLoader";

    private final Logger logger = LogManager.getLogger(getClass());

    private final @NonNull GithubService githubService;

    private final @NonNull HttpService httpService;

    private final @NonNull FileService fileService;

    private final @NonNull TerrariaInstanceRepository terrariaInstanceRepository;

    private final @NonNull HostRepository hostRepository;

    private static @NonNull String getTModLoaderFileOsString(final HostEntity host) throws InvalidInstanceException {
        final String tModLoaderFileOsString;
        switch (host.getOs()) {
            case WINDOWS -> tModLoaderFileOsString = "Windows";
            case MAC -> tModLoaderFileOsString = "Mac";
            case LINUX -> tModLoaderFileOsString = "Linux";
            default -> throw new InvalidInstanceException(
                    String.format("TModLoader is not supported for the [%s] operating system", host.getOs()));
        }
        return tModLoaderFileOsString;
    }

    private static @NonNull String getServerZipSubdirectory(final HostEntity host) throws InvalidInstanceException {
        final String serverZipSubdirectory;
        switch (host.getOs()) {
            case WINDOWS -> serverZipSubdirectory = "Windows";
            case MAC -> serverZipSubdirectory = "Mac";
            case LINUX -> serverZipSubdirectory = "Linux";
            default -> throw new InvalidInstanceException(
                    String.format("Cannot run the Terraria server on the [%s] operating system", host.getOs()));
        }
        return serverZipSubdirectory;
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
                .location(instanceDirectory)
                .name(creationModel.getInstanceName())
                .terrariaServerUrl(creationModel.getTerrariaServerArchiveUrl())
                .modLoaderReleaseId(creationModel.getModLoaderReleaseId())
                .state(TerrariaInstanceState.DEFINED)
                .pendingAction(TerrariaInstanceAction.SET_UP)
                .host(host)
                .build();

        final TerrariaInstanceEntity result = terrariaInstanceRepository.save(newTerrariaInstance);

        logger.info("Defined a Terraria instance at {}", instanceDirectory);
        return result;
    }

    @Synchronized
    TerrariaInstanceEntity setUpTerrariaInstance(TerrariaInstanceEntity instance)
            throws IOException, InvalidInstanceException {

        // Validation
        final String serverUrl = instance.getTerrariaServerUrl();
        final Matcher serverZipUrlMatcher = TERRARIA_SERVER_URL_REGEX.matcher(serverUrl);
        if (!serverZipUrlMatcher.find()) {
            throw new InvalidInstanceException(
                    "The URL " + serverUrl + " does not match the regular expression " + TERRARIA_SERVER_URL_REGEX);
        }

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

        final String serverZipName = serverZipUrlMatcher.group(2);
        final String serverRawVersion = serverZipUrlMatcher.group(3);

        final Path serverZipCacheLocation = Path.of(TERRARIA_SERVER_CACHE_DIR, serverZipName);
        if (!fileService.hasFileInCache(serverZipCacheLocation)) {
            try {
                httpService.checkUrl(new URL(serverUrl));
            } catch (final HttpClientErrorException e) {
                throw new InvalidInstanceException(
                        String.format("The response at URL %s is HTTP code %d: %s", serverUrl, e.getRawStatusCode(),
                                e.getStatusText()), e);
            }
        }

        instance = markInstanceAsValid(instance, serverRawVersion, tModLoaderRelease, tModLoaderAsset);

        // Creation
        final File instanceDirectory = fileService.reserveDirectory(instance.getLocation());
        final InputStreamSupplier serverFetcher = () -> httpService.requestAsStream(new URL(serverUrl));
        try (final InputStream serverZipStream = fileService.cache(serverZipCacheLocation, serverFetcher, null)) {
            fileService.unzip(serverZipStream, instanceDirectory,
                    Path.of(serverRawVersion, serverZipSubdirectory).toString());
        }

        try (final InputStream tModLoaderZipStream = githubService.fetchAsset(tModLoaderAsset)) {
            fileService.unzip(tModLoaderZipStream, instanceDirectory);
        }

        instance = markInstanceAsReady(instance);

        logger.info("Created a Terraria instance at {}", instanceDirectory.getAbsolutePath());
        return instance;
    }

    private TerrariaInstanceEntity markInstanceAsValid(final TerrariaInstanceEntity instance,
            final String serverRawVersion,
            final GitHubRelease tModLoaderRelease,
            final GitHubReleaseAsset tModLoaderAsset) throws NullPointerException {

        instance.setTerrariaVersion(String.join(".", serverRawVersion.split("")));
        instance.setModLoaderVersion(
                tModLoaderRelease.getName().substring(tModLoaderRelease.getName().startsWith("v") ? 1 : 0));
        instance.setModLoaderArchiveUrl(Objects.requireNonNull(tModLoaderAsset.getBrowserDownloadUrl()));
        instance.setModLoaderReleaseUrl(tModLoaderRelease.getHtmlUrl());
        instance.setState(TerrariaInstanceState.VALID);
        return terrariaInstanceRepository.save(instance);
    }

    private TerrariaInstanceEntity markInstanceAsReady(final TerrariaInstanceEntity instance) {
        instance.setState(TerrariaInstanceState.READY);
        instance.setPendingAction(null);
        return terrariaInstanceRepository.save(instance);
    }

    private @NonNull GitHubReleaseAsset getTModLoaderAsset(final @NonNull GitHubRelease tModLoaderRelease,
            final @NonNull String osString) throws NotFoundException {
        return tModLoaderRelease.getAssets()
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
