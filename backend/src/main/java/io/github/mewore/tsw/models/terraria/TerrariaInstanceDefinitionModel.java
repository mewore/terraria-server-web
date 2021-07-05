package io.github.mewore.tsw.models.terraria;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
@Getter
public class TerrariaInstanceDefinitionModel {

    private final @NotBlank @NonNull String instanceName;

    private final @Positive long modLoaderReleaseId;

    private final @NotBlank @NonNull String terrariaServerArchiveUrl;
}
