package io.github.mewore.tsw.services;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import io.github.mewore.tsw.models.github.GitHubRelease;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
class GitHubCacheItem {

    private static final Duration CACHE_VALIDITY_DURATION = Duration.ofHours(1);

    private final @lombok.NonNull Instant expiration;

    @Getter
    private final @lombok.NonNull List<GitHubRelease> releases;

    GitHubCacheItem(final List<GitHubRelease> releases) {
        this(Instant.now().plus(CACHE_VALIDITY_DURATION), releases);
    }

    public boolean isValid() {
        return Instant.now().isBefore(expiration);
    }
}
