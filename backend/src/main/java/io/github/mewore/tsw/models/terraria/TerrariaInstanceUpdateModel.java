package io.github.mewore.tsw.models.terraria;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Set;

import org.springframework.lang.Nullable;

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

    @Nullable
    private TerrariaInstanceAction newAction;

    @Nullable
    private @NullOrNotBlank String newName;

    @Nullable
    private Set<@NotNull String> newMods;

    @Nullable
    private @Valid TerrariaInstanceRunConfiguration runConfiguration;

    @Nullable
    private @Valid WorldCreationConfiguration worldCreationConfiguration;
}
