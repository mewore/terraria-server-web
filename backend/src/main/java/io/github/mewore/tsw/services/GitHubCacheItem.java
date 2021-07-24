package io.github.mewore.tsw.services;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import io.github.mewore.tsw.models.github.GitHubRelease;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
class GitHubCacheItem {

    private static final Duration CACHE_VALIDITY_DURATION = Duration.ofHours(1);

    private final @NonNull Instant expiration;

    @Getter
    private final @NonNull List<GitHubRelease> releases;

    GitHubCacheItem(final List<GitHubRelease> releases) {
        this(Instant.now().plus(CACHE_VALIDITY_DURATION), releases);
    }

    public boolean isValid() {
        return Instant.now().isBefore(expiration);
    }
}
