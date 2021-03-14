package io.github.mewore.tsw.services;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;

import org.springframework.stereotype.Service;

import io.github.mewore.tsw.models.github.GitHubRelease;
import io.github.mewore.tsw.models.terraria.TModLoaderVersionViewModel;
import io.github.mewore.tsw.services.util.HttpService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Service
public class TerrariaService {

    private static final String T_MOD_LOADER_RELEASE_URL =
            "https://api.github.com/repos/tModLoader/tModLoader/releases";

    private static final Duration T_MOD_LOADER_RELEASE_CACHE_DURATION = Duration.ofHours(1);

    private final HttpService httpService;

    private Instant tModLoaderReleaseFetchInstant = Instant.MIN;

    private List<TModLoaderVersionViewModel> tModLoaderVersions = Collections.emptyList();

    private void refreshTModLoaderGithubReleases() throws IOException {
        if (Instant.now().isAfter(tModLoaderReleaseFetchInstant)) {
            final List<GitHubRelease> tModLoaderGithubReleases = Collections.unmodifiableList(
                    httpService.get(new URL(T_MOD_LOADER_RELEASE_URL), new TypeReference<>() {
                    }));
            tModLoaderVersions = tModLoaderGithubReleases
                    .stream()
                    .map(release -> new TModLoaderVersionViewModel(release.getId(), release.getName()))
                    .collect(Collectors.toUnmodifiableList());
            tModLoaderReleaseFetchInstant = Instant.now().plus(T_MOD_LOADER_RELEASE_CACHE_DURATION);
        }
    }

    public List<TModLoaderVersionViewModel> fetchTModLoaderVersions() throws IOException {
        refreshTModLoaderGithubReleases();
        return tModLoaderVersions;
    }
}
