package io.github.mewore.tsw.models.terraria;

import java.time.Instant;
import java.util.Set;

import io.github.mewore.tsw.models.HostFactory;
import io.github.mewore.tsw.models.terraria.world.TerrariaWorldEntity;
import io.github.mewore.tsw.models.terraria.world.WorldDifficultyOption;
import io.github.mewore.tsw.models.terraria.world.WorldSizeOption;

public class TerrariaWorldFactory {

    public static final long WORLD_ID = 1;

    public static TerrariaWorldEntity makeWorld() {
        return makeWorldBuilder().build();
    }

    public static TerrariaWorldEntity.TerrariaWorldEntityBuilder makeWorldBuilder() {
        return TerrariaWorldEntity.builder()
                .id(WORLD_ID)
                .fileName("World_Name")
                .displayName("World Name")
                .lastModified(Instant.EPOCH)
                .mods(Set.of("mod"))
                .host(HostFactory.makeHost())
                .size(WorldSizeOption.MEDIUM)
                .difficulty(WorldDifficultyOption.NORMAL);
    }
}
