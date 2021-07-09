package io.github.mewore.tsw.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.exceptions.NotFoundException;
import io.github.mewore.tsw.models.github.GitHubRelease;
import io.github.mewore.tsw.models.github.GitHubReleaseAsset;
import io.github.mewore.tsw.services.util.FileService;
import io.github.mewore.tsw.services.util.HttpService;
import io.github.mewore.tsw.services.util.InputStreamSupplier;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GithubService {

    private static final String CACHE_DIRECTORY_NAME = "github-assets";

    private static final String GITHUB_API_URL = "https://api.github.com";

    private final Map<String, GitHubCacheItem> cache = new HashMap<>();

    private final HttpService httpService;

    private final FileService fileService;

    public @NonNull List<@NonNull GitHubRelease> getReleases(final String author, final String repository)
            throws IOException {

        final String cacheKey = author + "/" + repository;
        {
            final @Nullable GitHubCacheItem cacheItem = cache.get(cacheKey);
            if (cacheItem != null && cacheItem.isValid()) {
                return cacheItem.getReleases();
            }
        }

        final URL url = new URL(String.format("%s/repos/%s/%s/releases", GITHUB_API_URL, author, repository));
        final List<GitHubRelease> releases = Collections.unmodifiableList(httpService.get(url, new TypeReference<>() {
        }));
        cache.put(cacheKey, new GitHubCacheItem(releases));
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
        final String downloadUrl = asset.getBrowserDownloadUrl();
        final InputStreamSupplier downloadSupplier = () -> {
            if (downloadUrl == null) {
                throw new NullPointerException("The asset with ID " + asset.getId() + " does not have a download URL");
            }
            return httpService.requestAsStream(new URL(downloadUrl));
        };

        final Path cacheFile = Path.of(CACHE_DIRECTORY_NAME, asset.getId() + "-" + asset.getName());
        return fileService.cache(cacheFile, downloadSupplier, asset.getSize());
    }
}
