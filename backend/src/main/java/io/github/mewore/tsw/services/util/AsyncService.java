package io.github.mewore.tsw.services.util;

import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

/**
 * A wrapper of Java concurrency operations for easier mocking.
 */
@Service
public class AsyncService {

    public Future<?> scheduleAtFixedRate(final Runnable command, final Duration initialDelay, final Duration period) {

        return new ScheduledThreadPoolExecutor(1).scheduleAtFixedRate(command, initialDelay.toMillis(),
                period.toMillis(), TimeUnit.MILLISECONDS);
    }

    public void runInThread(final Runnable target) {
        final Thread thread = new Thread(target);
        thread.start();
    }
}
