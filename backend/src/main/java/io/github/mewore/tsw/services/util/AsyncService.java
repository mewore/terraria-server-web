package io.github.mewore.tsw.services.util;

import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

/**
 * A wrapper of Java concurrency operations for easier mocking.
 */
@Service
public class AsyncService {

    private final Logger logger = LogManager.getLogger(getClass());

    public Future<?> scheduleAtFixedRate(final Runnable command, final Duration initialDelay, final Duration period) {

        return new ScheduledThreadPoolExecutor(1).scheduleAtFixedRate(command, initialDelay.toMillis(),
                period.toMillis(), TimeUnit.MILLISECONDS);
    }

    public void runInThread(final Runnable target) {
        final Thread thread = new Thread(target);
        thread.start();
    }

    public void runContinuously(final InterruptableRunnable target) {
        final Thread thread = new Thread(() -> {
            while (true) {
                try {
                    target.run();
                } catch (final InterruptedException e) {
                    logger.warn("Continuous thread interrupted", e);
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        thread.start();
    }

    @FunctionalInterface
    public interface InterruptableRunnable {

        void run() throws InterruptedException;
    }
}
