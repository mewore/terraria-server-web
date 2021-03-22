package io.github.mewore.tsw.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.type.TypeReference;

import org.springframework.stereotype.Service;

import io.github.mewore.tsw.exceptions.NotFoundException;
import io.github.mewore.tsw.models.github.GitHubRelease;
import io.github.mewore.tsw.models.github.GitHubReleaseAsset;
import io.github.mewore.tsw.services.util.FileService;
import io.github.mewore.tsw.services.util.HttpService;
import io.github.mewore.tsw.services.util.InputStreamSupplier;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GithubService {

    private static final String CACHE_DIRECTORY_NAME = "github-assets";

    private static final String GITHUB_API_URL = "https://api.github.com";

    private static final Duration RELEASE_CACHE_DURATION = Duration.ofHours(1);

    private final @NonNull Map<@NonNull String, @NonNull Instant> cacheExpirationMap = new HashMap<>();

    private final @NonNull Map<@NonNull String, @NonNull List<@NonNull GitHubRelease>> cache = new HashMap<>();

    private final @NonNull HttpService httpService;

    private final @NonNull FileService fileService;

    public @NonNull List<@NonNull GitHubRelease> getReleases(final String author, final String repository)
            throws IOException {

        final String cacheKey = author + "/" + repository;
        if (Instant.now().isBefore(cacheExpirationMap.getOrDefault(cacheKey, Instant.MIN))) {
            return cache.get(cacheKey);
        }

        final URL url = new URL(String.format("%s/repos/%s/%s/releases", GITHUB_API_URL, author, repository));
        final List<GitHubRelease> releases = Collections.unmodifiableList(httpService.get(url, new TypeReference<>() {
        }));
        cache.put(cacheKey, releases);
        cacheExpirationMap.put(cacheKey, Instant.now().plus(RELEASE_CACHE_DURATION));
        return releases;
    }

    public @NonNull GitHubRelease getRelease(final String author, final String repository, final long id)
            throws IOException, NotFoundException {

        return getReleases(author, repository)
                .stream()
                .filter(release -> release.getId() == id)
                .findAny()
                .orElseThrow(() -> new NotFoundException("No " + repository + " release with ID " + id));
    }

    public @NonNull InputStream fetchAsset(final GitHubReleaseAsset asset) throws IOException {
        final Path cacheFile = Path.of(CACHE_DIRECTORY_NAME, asset.getId() + "-" + asset.getName());
        final InputStreamSupplier downloadSupplier = () -> httpService.requestAsStream(
                new URL(Objects.requireNonNull(asset.getBrowserDownloadUrl(),
                        "The asset with ID " + asset.getId() + " does not have a download URL")));
        return fileService.cache(cacheFile, downloadSupplier, asset.getSize());
    }
}
