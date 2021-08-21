package io.github.mewore.tsw.services.util.async;

import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * A thread that continues executing until the end of its lifecycle. It can be run only once, most likely in the
 * setUp method of the consumer component.
 */
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class LifecycleThreadPool {

    private final Logger logger = LogManager.getLogger(getClass());

    private final AtomicBoolean canBeRun = new AtomicBoolean(true);

    private volatile @Nullable ExecutorService executor = null;

    public void run(final InterruptableRunnable... runnableArray) {
        if (!canBeRun.compareAndSet(true, false)) {
            throw new IllegalStateException("Cannot run a lifecycle thread more than once");
        }
        final ExecutorService newExecutor = Executors.newFixedThreadPool(runnableArray.length);
        executor = newExecutor;
        for (final InterruptableRunnable runnable : runnableArray) {
            newExecutor.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        runnable.run();
                    } catch (final InterruptedException e) {
                        logger.warn("Lifecycle thread interrupted", e);
                        Thread.currentThread().interrupt();
                        return;
                    } catch (final RuntimeException e) {
                        logger.warn("Error thrown in a lifecycle thread", e);
                    }
                }
            });
        }
    }

    @PreDestroy
    void preDestroy() {
        canBeRun.set(false);
        final ExecutorService currentExecutor = executor;
        if (currentExecutor != null) {
            currentExecutor.shutdownNow();
        }
    }

}
