package io.github.mewore.tsw.models.github;

import java.util.List;

import org.springframework.lang.Nullable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class GitHubRelease {

    private @NonNull List<@NonNull GitHubReleaseAsset> assets;

    private long id;

    private @NonNull String name;

    @Nullable
    private String url;
}




