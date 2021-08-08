package io.github.mewore.tsw.models.terraria;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class TerrariaInstanceRunConfiguration {

    private final @PositiveOrZero int maxPlayers;

    private final @Min(1024) @Max(49151) int port;

    private final boolean automaticallyForwardPort;

    private final String password;

    private final @Positive long worldId;
}
