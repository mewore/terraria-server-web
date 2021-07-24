package io.github.mewore.tsw.services.util.process;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Service
public class TmuxService {

    private static final Duration NEW_SESSION_TIMEOUT = Duration.ofSeconds(60);

    private static final Duration KILL_SESSION_TIMEOUT = Duration.ofMinutes(5);

    private static final Duration HAS_SESSION_TIMEOUT = Duration.ofSeconds(10);

    private static final Duration SEND_KEYS_TIMEOUT = Duration.ofSeconds(10);

    private static final int SUCCESS_STATUS = 0;

    private final Logger logger = LogManager.getLogger(getClass());

    private static String characterToKeyParameter(final char character) {
        switch (character) {
            case ' ': {
                return "' '";
            }
            case '\n': {
                return "Enter";
            }
            case '\t': {
                return "Tab";
            }
            default: {
                return String.valueOf(character);
            }
        }
    }

    private static void checkProcessStatus(final Process process) throws ProcessFailureException {
        if (process.exitValue() != 0) {
            throw new ProcessFailureException(process);
        }
    }

    public void dispatch(final String sessionName, final File program, final File outputFile)
            throws ProcessTimeoutException, InterruptedException, ProcessFailureException {
        final String outputFilePath = outputFile.getAbsolutePath();
        final String bashCommand = String.format(
                "tmux new-session -d -s %s \"echo -n 'Working directory: ' && pwd && " +
                        "trap 'cp \\\"%s\\\" \\\"%s.\\$(date -u +%%Y-%%m-%%dT%%H-%%M-%%S.%%N).log\\\"; rm \\\"%s\\\"'" +
                        " EXIT && " + "'%s' 2>&1 | tee '%s';\"", sessionName, outputFilePath, outputFilePath,
                outputFilePath, program.getAbsolutePath(), outputFilePath);
        final ProcessTracker processTracker = runAndTrackBashCommand(bashCommand);
        processTracker.runWithTimeout(NEW_SESSION_TIMEOUT);
        checkProcessStatus(processTracker.getProcess());
    }

    public void kill(final String sessionName)
            throws ProcessTimeoutException, InterruptedException, ProcessFailureException {
        final String bashCommand = String.format("tmux kill-session -t %s", sessionName);
        final ProcessTracker processTracker = runAndTrackBashCommand(bashCommand);
        processTracker.runWithTimeout(KILL_SESSION_TIMEOUT);
        checkProcessStatus(processTracker.getProcess());
    }

    public boolean hasSession(final String sessionName)
            throws ProcessTimeoutException, InterruptedException, ProcessFailureException {
        final ProcessTracker processTracker = runAndTrackBashCommand(
                String.format("tmux has-session -t %s", sessionName));
        return processTracker.runWithTimeout(HAS_SESSION_TIMEOUT) == SUCCESS_STATUS;
    }

    public void sendInput(final String sessionName, final String keys)
            throws ProcessTimeoutException, InterruptedException, ProcessFailureException {
        sendKeys(sessionName, keys.chars()
                .mapToObj(character -> characterToKeyParameter((char) character))
                .collect(Collectors.joining(" ")));
    }

    public void sendCtrlC(final String sessionName)
            throws ProcessTimeoutException, InterruptedException, ProcessFailureException {
        sendKeys(sessionName, "^C");
    }

    private void sendKeys(final String sessionName, final String keyParameterString)
            throws ProcessTimeoutException, InterruptedException, ProcessFailureException {
        final ProcessTracker processTracker = runAndTrackBashCommand(
                String.format("tmux send-keys -t %s:0.0 %s", sessionName, keyParameterString));
        processTracker.runWithTimeout(SEND_KEYS_TIMEOUT);
    }

    private ProcessTracker runAndTrackBashCommand(final String bashCommand) throws ProcessFailureException {
        logger.info("Executing in Bash: " + bashCommand);
        final Process process;
        try {
            process = new ProcessBuilder().command("/bin/bash", "-c", bashCommand).redirectErrorStream(true).start();
        } catch (final IOException e) {
            throw new ProcessFailureException("Failed to start a bash process with command: " + bashCommand, e);
        }
        return new ProcessTracker(
                LogManager.getLogger(String.format("Process [%d] (bash: %s)", process.pid(), bashCommand)), process);
    }
}
