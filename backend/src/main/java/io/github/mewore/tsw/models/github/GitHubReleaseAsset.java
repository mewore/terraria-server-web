package io.github.mewore.tsw.models.github;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class GitHubReleaseAsset {

    @JsonProperty("browser_download_url")
    private @Nullable String browserDownloadUrl;

    @JsonProperty("content_type")
    private @Nullable String contentType;

    private long id;

    private @NonNull String name;

    private @Nullable Long size;
}
