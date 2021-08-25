package io.github.mewore.tsw.events;

import java.io.Serializable;
import java.util.UUID;

import io.github.mewore.tsw.models.terraria.world.TerrariaWorldEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public class DeletedWorldNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long id;

    private final @NonNull String fileName;

    private final @NonNull String displayName;

    private final @NonNull UUID hostUuid;

    public DeletedWorldNotification(final TerrariaWorldEntity world) {
        this(world.getId(), world.getFileName(), world.getDisplayName(), world.getHost().getUuid());
    }
}
