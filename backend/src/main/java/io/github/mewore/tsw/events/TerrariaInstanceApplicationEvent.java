package io.github.mewore.tsw.events;

import org.springframework.context.ApplicationEvent;

import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import lombok.Getter;

@Getter
public class TerrariaInstanceApplicationEvent extends ApplicationEvent {

    private final TerrariaInstanceEntity changedInstance;

    private final boolean isNew;

    public TerrariaInstanceApplicationEvent(final TerrariaInstanceEntity changedInstance, final boolean isNew) {
        super(changedInstance);
        this.changedInstance = changedInstance;
        this.isNew = isNew;
    }
}
