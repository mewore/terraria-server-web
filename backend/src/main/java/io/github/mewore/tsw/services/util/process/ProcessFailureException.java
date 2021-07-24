package io.github.mewore.tsw.services.util.process;

public class ProcessFailureException extends Exception {

    ProcessFailureException(final Process process) {
        super(String.format("Process [%d] has failed. It has exited with status %d.", process.pid(),
                process.exitValue()));
    }

    ProcessFailureException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
