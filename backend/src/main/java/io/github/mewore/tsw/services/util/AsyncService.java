package io.github.mewore.tsw.services.util;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

/**
 * A wrapper of Java concurrency operations for easier mocking.
 */
@Service
public class AsyncService {

    public final ExecutorService commonExecutor = Executors.newCachedThreadPool();

    public Future<?> scheduleAtFixedRate(final Runnable command, final Duration initialDelay, final Duration period) {

        return new ScheduledThreadPoolExecutor(1).scheduleAtFixedRate(command, initialDelay.toMillis(),
                period.toMillis(), TimeUnit.MILLISECONDS);
    }

    public void runInThread(final Runnable target) {
        commonExecutor.submit(target);
    }

    @PreDestroy
    void preDestroy() {
        commonExecutor.shutdownNow();
    }
}
