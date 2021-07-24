package io.github.mewore.tsw.events;

import org.springframework.context.ApplicationEvent;

import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import lombok.Getter;

@Getter
public class TerrariaInstanceUpdatedEvent extends ApplicationEvent {

    private final TerrariaInstanceEntity changedInstance;

    public TerrariaInstanceUpdatedEvent(final TerrariaInstanceEntity changedInstance) {
        super(changedInstance);
        this.changedInstance = changedInstance;
    }
}
