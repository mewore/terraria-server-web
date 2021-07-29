package io.github.mewore.tsw.services.util;

public interface FileTailEventConsumer {

    void onFileCreated();

    void onReadStarted();

    void onCharacter(final char character, final long position);

    void onReadFinished(final long position);

    void onFileDeleted();
}
