package io.github.mewore.tsw.models.terraria.world;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum WorldSizeOption {
    SMALL("Small"),
    MEDIUM("Medium"),
    LARGE("Large");

    private final String optionLabel;
}
