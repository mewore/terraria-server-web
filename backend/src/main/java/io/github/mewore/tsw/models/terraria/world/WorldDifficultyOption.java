package io.github.mewore.tsw.models.terraria.world;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum WorldDifficultyOption {
    NORMAL("Normal"),
    EXPERT("Expert");

    private final String optionLabel;
}
