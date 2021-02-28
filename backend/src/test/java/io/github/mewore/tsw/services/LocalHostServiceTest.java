package io.github.mewore.tsw.services;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.repositories.HostRepository;
import io.github.mewore.tsw.services.util.AsyncService;
import io.github.mewore.tsw.services.util.FileService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalHostServiceTest {

    private static final String UUID_FILE_CONTENTS = """

            e0f245dc-e6e4-4f8a-982b-004cbb04e505

            """;

    private static final UUID HOST_UUID = UUID.fromString("e0f245dc-e6e4-4f8a-982b-004cbb04e505");

    private static final String HOST_URL = "host-url";

    private static final HostEntity EXISTING_HOST = HostEntity.builder()
            .uuid(HOST_UUID)
            .url(HOST_URL)
            .alive(false)
            .build();

    @Mock
    private HostRepository hostRepository;

    @Mock
    private FileService fileService;

    @Mock
    private AsyncService asyncService;

    @Mock
    private Future<?> future;

    @Captor
    private ArgumentCaptor<HostEntity> hostCaptor;

    @Captor
    private ArgumentCaptor<Runnable> heartbeatCaptor;

    @Captor
    private ArgumentCaptor<Duration> heartbeatDelayCaptor;

    @Captor
    private ArgumentCaptor<Duration> heartbeatPeriodCaptor;

    @Test
    void testInitialization() throws IOException {
        when(fileService.fileExists(any())).thenReturn(true);
        when(fileService.readFile(any())).thenReturn(UUID_FILE_CONTENTS);
        when(hostRepository.findByUuid(HOST_UUID)).thenReturn(Optional.of(EXISTING_HOST));
        when(asyncService.scheduleAtFixedRate(any(), any(), any())).thenAnswer(invocation -> future);

        new LocalHostService(hostRepository, fileService, asyncService, HOST_URL);

        verify(asyncService, only()).scheduleAtFixedRate(any(), heartbeatDelayCaptor.capture(),
                heartbeatPeriodCaptor.capture());
        assertEquals(Duration.ZERO, heartbeatDelayCaptor.getValue());
        assertEquals(Duration.ofMinutes(1), heartbeatPeriodCaptor.getValue());
    }

    @Test
    void testInitialization_noUuidFile() throws IOException {
        when(fileService.fileExists(any())).thenReturn(false);
        when(asyncService.scheduleAtFixedRate(any(), any(), any())).thenAnswer(invocation -> future);

        new LocalHostService(hostRepository, fileService, asyncService, HOST_URL);
        verify(fileService).makeFile(any(), any());
        verify(hostRepository, never()).findByUuid(any());
    }

    @Test
    void testInitialization_invalidUuidFile() throws IOException {
        when(fileService.fileExists(any())).thenReturn(true);
        when(fileService.readFile(any())).thenReturn("Invalid UUID string");
        when(asyncService.scheduleAtFixedRate(any(), any(), any())).thenAnswer(invocation -> future);

        new LocalHostService(hostRepository, fileService, asyncService, HOST_URL);
        verify(fileService).makeFile(any(), any());
        verify(hostRepository, never()).findByUuid(any());
    }

    @Test
    void testHeartbeat() throws IOException {
        final Runnable heartbeat = prepareHeartbeat();
        when(hostRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        heartbeat.run();
        verify(hostRepository).save(hostCaptor.capture());
        assertTrue(hostCaptor.getValue().isAlive());
        assertEquals(HOST_UUID, hostCaptor.getValue().getUuid());
        assertEquals(HOST_URL, hostCaptor.getValue().getUrl());
    }

    private Runnable prepareHeartbeat() throws IOException {
        when(fileService.fileExists(any())).thenReturn(true);
        when(fileService.readFile(any())).thenReturn(UUID_FILE_CONTENTS);
        when(hostRepository.findByUuid(HOST_UUID)).thenReturn(Optional.of(EXISTING_HOST));
        when(asyncService.scheduleAtFixedRate(any(), any(), any())).thenAnswer(invocation -> future);

        new LocalHostService(hostRepository, fileService, asyncService, HOST_URL);
        verify(asyncService, only()).scheduleAtFixedRate(heartbeatCaptor.capture(), any(), any());
        return heartbeatCaptor.getValue();
    }

    @Test
    void testPreDestroy() throws IOException {
        when(fileService.fileExists(any())).thenReturn(true);
        when(fileService.readFile(any())).thenReturn(UUID_FILE_CONTENTS);
        when(hostRepository.findByUuid(any())).thenReturn(
                Optional.of(HostEntity.builder().alive(true).lastHeartbeat(Instant.MAX).build()));
        when(asyncService.scheduleAtFixedRate(any(), any(), any())).thenAnswer(invocation -> future);

        new LocalHostService(hostRepository, fileService, asyncService, HOST_URL).preDestroy();
        verify(future, only()).cancel(false);
        verify(hostRepository).save(hostCaptor.capture());
        assertFalse(hostCaptor.getValue().isAlive());
    }
}