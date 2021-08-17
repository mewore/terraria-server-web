package io.github.mewore.tsw.models.terraria;

import io.github.mewore.tsw.models.MessageModel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@MessageModel
public class TerrariaInstanceEventMessage {

    private final long id;

    private final @NonNull TerrariaInstanceEventType type;

    private final @NonNull String content;

    public TerrariaInstanceEventMessage(final TerrariaInstanceEventEntity event) {
        this(event.getId(), event.getType(), event.getContent());
    }
}
