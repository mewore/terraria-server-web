package io.github.mewore.tsw.services.util.process;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class ProcessTracker implements Runnable {

    private final Logger logger;

    @Getter
    private final Process process;

    @Getter
    private List<String> outputLines = Collections.emptyList();

    private static List<String> logAndCollectStreamLines(final Logger logger, final InputStream stream)
            throws IOException {
        final List<String> result = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        while (true) {
            final int b;
            b = stream.read();
            if (b < 0) {
                break;
            }
            final char c = (char) b;
            if (c == '\n') {
                final String line = stringBuilder.toString();
                logger.info(line);
                result.add(line);
                stringBuilder = new StringBuilder();
            } else {
                stringBuilder.append(c);
            }
        }
        if (stringBuilder.length() > 0) {
            final String line = stringBuilder.toString();
            logger.info(line);
            result.add(line);
        }
        return result;
    }

    @Override
    public void run() {
        try {
            this.track(null);
        } catch (final InterruptedException e) {
            logger.warn("Interrupted while getting the status of process [{}]", process.pid());
            Thread.currentThread().interrupt();
        }
    }

    public int runWithTimeout(final Duration timeout) throws InterruptedException, ProcessTimeoutException {
        final @Nullable Integer exitStatus = this.track(timeout);
        if (exitStatus == null) {
            throw new ProcessTimeoutException(process, timeout);
        }
        return exitStatus;
    }

    private @Nullable Integer track(final @Nullable Duration timeout) throws InterruptedException {
        logger.info("Started tracking process [{}]", process.pid());

        try {
            outputLines = Collections.unmodifiableList(logAndCollectStreamLines(logger, process.getInputStream()));
        } catch (final IOException e) {
            logger.error("Encountered an exception while tracking the output lines of the process!", e);
        }

        if (timeout != null && !process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            logger.error("The process did not finish in a timeout of {}", timeout);
            return null;
        }

        final int status = process.waitFor();
        logger.info("Finished with status: {}", status);
        return status;
    }
}
