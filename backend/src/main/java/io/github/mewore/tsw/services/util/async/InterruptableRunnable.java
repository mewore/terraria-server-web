package io.github.mewore.tsw.services.util.async;

@FunctionalInterface
public interface InterruptableRunnable {

    /**
     * @throws InterruptedException If interrupted while executing a blocking operation.
     */
    void run() throws InterruptedException;
}
