package io.github.mewore.tsw.services.util.process;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessTrackerTest {

    @InjectMocks
    private ProcessTracker processTracker;

    @Mock
    private Logger logger;

    @Mock
    private Process process;

    @Captor
    private ArgumentCaptor<String> singleStringLogCaptor;

    @Test
    void testRun() throws InterruptedException {
        final String[] lines = new String[]{"First line", "", " ", "Second line", "Third line"};
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream(String.join("\n", lines).getBytes()));
        when(process.pid()).thenReturn(11L);
        when(process.waitFor()).thenReturn(18);
        processTracker.run();

        verify(logger).info("Started tracking process [{}]", 11L);

        verify(logger, times(5)).info(singleStringLogCaptor.capture());
        final List<String> singleStringInfoCalls = singleStringLogCaptor.getAllValues();
        assertArrayEquals(lines, singleStringInfoCalls.toArray(new String[0]));
        assertEquals(singleStringInfoCalls, processTracker.getOutputLines());

        verify(logger).info("Finished with status: {}", 18);
    }

    @Test
    void testRun_interrupted() throws InterruptedException {
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(process.waitFor()).thenThrow(new InterruptedException());
        when(process.pid()).thenReturn(11L);
        processTracker.run();

        verify(logger).warn("Interrupted while getting the status of process [{}]", 11L);
    }

    @Test
    void testRunWithTimeout() throws InterruptedException, ProcessTimeoutException {
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(process.pid()).thenReturn(11L);
        when(process.waitFor(5000L, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(process.waitFor()).thenReturn(18);
        processTracker.runWithTimeout(Duration.ofSeconds(5));
    }

    @Test
    void testRunWithTimeout_timeout() throws InterruptedException {
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(process.pid()).thenReturn(11L);
        when(process.waitFor(5000L, TimeUnit.MILLISECONDS)).thenReturn(false);

        final Exception exception = assertThrows(ProcessTimeoutException.class,
                () -> processTracker.runWithTimeout(Duration.ofSeconds(5)));
        assertEquals("Process [11] did not finish execution after a timeout of PT5S", exception.getMessage());
        verify(process, never()).waitFor();
    }
}