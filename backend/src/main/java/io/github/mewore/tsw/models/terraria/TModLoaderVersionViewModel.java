package io.github.mewore.tsw.models.terraria;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TModLoaderVersionViewModel {

    private final long releaseId;

    private final String version;
}
