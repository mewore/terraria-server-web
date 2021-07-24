package io.github.mewore.tsw.services.util.process;

import java.time.Duration;

public class ProcessTimeoutException extends Exception {

    ProcessTimeoutException(final Process process, final Duration timeout) {
        super(String.format("Process [%d] did not finish execution after a timeout of %s", process.pid(), timeout));
    }
}
