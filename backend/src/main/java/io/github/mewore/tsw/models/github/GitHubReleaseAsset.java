package io.github.mewore.tsw.models.github;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.lang.Nullable;

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

    @Nullable
    @JsonProperty("browser_download_url")
    private String browserDownloadUrl;

    @Nullable
    @JsonProperty("content_type")
    private String contentType;

    private long id;

    private @NonNull String name;

    @Nullable
    private Long size;
}
