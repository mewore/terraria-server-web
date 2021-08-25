package io.github.mewore.tsw.events;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

@Getter
public class TerrariaWorldDeletionEvent extends ApplicationEvent {

    private final DeletedWorldNotification deletedWorld;

    public TerrariaWorldDeletionEvent(final DeletedWorldNotification deletedWorld) {
        super(deletedWorld);
        this.deletedWorld = deletedWorld;
    }
}
