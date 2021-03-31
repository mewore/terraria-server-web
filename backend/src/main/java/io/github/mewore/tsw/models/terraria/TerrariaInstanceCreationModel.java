package io.github.mewore.tsw.models.terraria;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TerrariaInstanceCreationModel {

    private final String instanceName;

    private final long hostId;

    private final long modLoaderReleaseId;

    private final String terrariaServerArchiveUrl;
}
