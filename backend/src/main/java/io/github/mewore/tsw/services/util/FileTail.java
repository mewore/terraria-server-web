package io.github.mewore.tsw.services.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.RequiredArgsConstructor;
import lombok.Synchronized;

@RequiredArgsConstructor
public class FileTail implements Runnable {

    private final File file;

    private final long startPosition;

    private final FileTailEventConsumer eventConsumer;

    private Logger logger = LogManager.getLogger(getClass());

    private long position;

    private volatile boolean shouldProcessEvents = true;

    private volatile boolean shouldReadFile = true;

    @Override
    public void run() {
        position = startPosition;
        logger = LogManager.getLogger(
                String.format("%s of file %s", getClass().getSimpleName(), file.getAbsolutePath()));
        final @Nullable File parent = file.getParentFile();
        if (parent == null) {
            logger.error("File {} does not have a parent! Cannot tail it.", file.getAbsolutePath());
            return;
        }
        try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
            if (file.exists()) {
                readFile();
            }

            WatchKey watchKey = parent.toPath()
                    .register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY,
                            StandardWatchEventKinds.ENTRY_DELETE);
            while (watchKey.isValid()) {
                watchKey = watchService.take();
                if (!shouldProcessEvents || !processEvents(watchKey) || !watchKey.reset()) {
                    break;
                }
            }
        } catch (final InterruptedException e) {
            logger.warn("Interrupted while watching for changes to the file.", e);
            Thread.currentThread().interrupt();
        } catch (final IOException | RuntimeException e) {
            logger.error("Encountered an unexpected exception while tailing the file.", e);
        }
    }

    /**
     * Do not read the file anymore.
     */
    @Synchronized
    public void stopReadingFile() {
        shouldReadFile = false;
    }


    /**
     * Do not process any events anymore.
     */
    @Synchronized
    public void stop() {
        shouldProcessEvents = false;
    }

    /**
     * Process the events of a watch key.
     *
     * @param watchKey The watch key to process the events of.
     * @return Whether watching should continue.
     * @throws InterruptedException If a blocking operation is interrupted.
     */
    @Synchronized
    private boolean processEvents(final WatchKey watchKey) throws InterruptedException {
        for (final WatchEvent<?> event : watchKey.pollEvents()) {
            final Path eventPath = (Path) event.context();
            if (eventPath == null || !eventPath.toString().equals(file.getName())) {
                continue;
            }
            final WatchEvent.Kind<?> kind = event.kind();

            try {
                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    eventConsumer.onFileCreated();
                    readFile();
                } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    readFile();
                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    eventConsumer.onFileDeleted();
                } else {
                    logger.warn("Unexpected event kind encountered for file {}: {}", file.getAbsolutePath(),
                            kind.name());
                }
            } catch (final RuntimeException e) {
                logger.error("Encountered an exception while handling a [" + kind.name() + "] event of file " +
                        file.getAbsolutePath(), e);
            }
        }
        return true;
    }

    @Synchronized
    private void readFile() throws InterruptedException {
        if (!shouldReadFile) {
            logger.info("Skipping reading file " + file.getAbsolutePath());
            return;
        }
        try (final FileInputStream inputStream = new FileInputStream(file)) {
            eventConsumer.onReadStarted();
            final long skippedBytes = inputStream.skip(position);
            if (skippedBytes < position) {
                logger.warn("Tried to skip {} bytes in the file {} but only {} were skipped!", position,
                        file.getAbsolutePath(), skippedBytes);
            }
            int b = inputStream.read();
            while (b >= 0) {
                eventConsumer.onCharacter((char) b, position++);
                b = inputStream.read();
            }
        } catch (final FileNotFoundException e) {
            logger.error("File not found!", e);
        } catch (final IOException | RuntimeException e) {
            logger.error("Encountered an IOException while reading the lines of file " + file.getAbsolutePath(), e);
        }
        eventConsumer.onReadFinished(position);
    }
}
