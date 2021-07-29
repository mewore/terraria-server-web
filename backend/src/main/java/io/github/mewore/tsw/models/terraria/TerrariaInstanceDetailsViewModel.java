package io.github.mewore.tsw.models.terraria;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class TerrariaInstanceDetailsViewModel {

    private final TerrariaInstanceEntity instance;

    private final List<TerrariaInstanceEventEntity> events;
}
