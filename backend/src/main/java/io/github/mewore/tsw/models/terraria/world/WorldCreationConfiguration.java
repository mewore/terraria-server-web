package io.github.mewore.tsw.models.terraria.world;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class WorldCreationConfiguration {

    private final @NotNull WorldSizeOption worldSize;

    private final @NotNull WorldDifficultyOption worldDifficulty;

    private final @NotNull @Pattern(regexp = "^[^\n]+$") String worldDisplayName;
}
