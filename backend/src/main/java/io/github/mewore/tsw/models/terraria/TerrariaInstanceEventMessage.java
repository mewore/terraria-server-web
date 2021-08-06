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

    private final @NonNull TerrariaInstanceEventType type;

    private final @NonNull String text;

    public TerrariaInstanceEventMessage(final TerrariaInstanceEventEntity event) {
        this(event.getType(), event.getText());
    }
}
