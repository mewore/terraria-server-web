package io.github.mewore.tsw.models.terraria;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.github.mewore.tsw.javax.validation.NullOrNotBlank;
import io.github.mewore.tsw.models.terraria.world.WorldCreationConfiguration;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Builder
@Getter
public class TerrariaInstanceUpdateModel {

    private @Nullable TerrariaInstanceAction newAction;

    private @Nullable @NullOrNotBlank String newName;

    private @Nullable Set<@NotNull String> newMods;

    private @Valid @Nullable TerrariaInstanceRunConfiguration runConfiguration;

    private @Valid @Nullable WorldCreationConfiguration worldCreationConfiguration;
}
