package io.github.mewore.tsw.events;

import org.springframework.context.ApplicationEvent;

import io.github.mewore.tsw.models.terraria.world.TerrariaWorldEntity;
import lombok.Getter;

@Getter
public class TerrariaWorldDeletionEvent extends ApplicationEvent {

    private final TerrariaWorldEntity deletedWorld;

    public TerrariaWorldDeletionEvent(final TerrariaWorldEntity deletedWorld) {
        super(deletedWorld);
        this.deletedWorld = deletedWorld;
    }
}
