package io.github.mewore.tsw.services.terraria;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

import io.github.mewore.tsw.models.terraria.TerrariaWorldEntity;
import io.github.mewore.tsw.models.terraria.TerrariaWorldFileEntity;
import io.github.mewore.tsw.services.util.FileService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class TerrariaWorldInfo {

    @Getter
    private final String name;

    @Getter
    private final Instant lastModified;

    private final File wldFile;

    private final File twldFile;

    private final FileService fileService;

    public TerrariaWorldFileEntity readFile(final TerrariaWorldEntity world) throws IOException {
        return TerrariaWorldFileEntity.builder()
                .name(name + ".zip")
                .content(fileService.zip(wldFile, twldFile))
                .world(world)
                .build();
    }
}
