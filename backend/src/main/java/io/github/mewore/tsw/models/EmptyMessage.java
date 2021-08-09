package io.github.mewore.tsw.models;

import org.springframework.messaging.support.GenericMessage;

public class EmptyMessage extends GenericMessage<byte[]> {

    public EmptyMessage() {
        super(new byte[0]);
    }
}
