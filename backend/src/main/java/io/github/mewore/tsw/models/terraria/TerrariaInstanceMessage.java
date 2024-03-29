package io.github.mewore.tsw.models.terraria;

import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.github.mewore.tsw.models.MessageModel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@MessageModel
public class TerrariaInstanceMessage {

    private final @Nullable String terrariaVersion;

    private final @Nullable String modLoaderVersion;

    private final @Nullable String modLoaderReleaseUrl;

    private final @Nullable String modLoaderArchiveUrl;

    private final @NonNull TerrariaInstanceState state;

    private final @Nullable TerrariaInstanceAction pendingAction;

    private final @Nullable TerrariaInstanceAction currentAction;

    private final @NonNull Map<Integer, String> options;

    public TerrariaInstanceMessage(final TerrariaInstanceEntity instance) {
        this(instance.getTerrariaVersion(), instance.getModLoaderVersion(), instance.getModLoaderReleaseUrl(),
                instance.getModLoaderArchiveUrl(), instance.getState(), instance.getPendingAction(),
                instance.getCurrentAction(), instance.getOptions());
    }
}
