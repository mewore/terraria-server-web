package io.github.mewore.tsw.models.github;

import java.util.List;

import org.springframework.lang.Nullable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class GitHubRelease {

    @Nullable
    private List<GitHubReleaseAsset> assets;

    private long id;

    @Nullable
    private String name;

    @Nullable
    private String url;
}




