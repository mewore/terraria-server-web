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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.file.OperatingSystem;
import io.github.mewore.tsw.repositories.HostRepository;
import io.github.mewore.tsw.services.util.AsyncService;
import io.github.mewore.tsw.services.util.FileService;
import io.github.mewore.tsw.services.util.SystemService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalHostServiceTest {

    private static final String UUID_FILE_CONTENTS = "\n\n e0f245dc-e6e4-4f8a-982b-004cbb04e505 \n\n";

    private static final UUID HOST_UUID = UUID.fromString("e0f245dc-e6e4-4f8a-982b-004cbb04e505");

    private static final String HOST_URL = "host-url";

    @InjectMocks
    private LocalHostService localHostService;

    @Mock
    private HostRepository hostRepository;

    @Mock
    private FileService fileService;

    @Mock
    private AsyncService asyncService;

    @Mock
    private SystemService systemService;

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
    void testSetUp() throws IOException {
        when(fileService.fileExists(any())).thenReturn(true);
        when(fileService.readFile(any())).thenReturn(UUID_FILE_CONTENTS);
        when(asyncService.scheduleAtFixedRate(any(), any(), any())).thenAnswer(invocation -> future);

        localHostService.setUp();
        verify(asyncService, only()).scheduleAtFixedRate(any(), heartbeatDelayCaptor.capture(),
                heartbeatPeriodCaptor.capture());
        assertEquals(Duration.ZERO, heartbeatDelayCaptor.getValue());
        assertEquals(Duration.ofMinutes(1), heartbeatPeriodCaptor.getValue());
        assertEquals(HOST_UUID, localHostService.getHostUuid());
    }

    @Test
    void testSetUp_noUuidFile() throws IOException {
        when(fileService.fileExists(any())).thenReturn(false);
        when(asyncService.scheduleAtFixedRate(any(), any(), any())).thenAnswer(invocation -> future);

        localHostService.setUp();
        verify(fileService).makeFile(any(), any());
        verify(hostRepository, never()).findByUuid(any());
    }

    @Test
    void testSetUp_invalidUuidFile() throws IOException {
        when(fileService.fileExists(any())).thenReturn(true);
        when(fileService.readFile(any())).thenReturn("Invalid UUID string");
        when(asyncService.scheduleAtFixedRate(any(), any(), any())).thenAnswer(invocation -> future);

        localHostService.setUp();
        verify(fileService).makeFile(any(), any());
        verify(hostRepository, never()).findByUuid(any());
    }

    private static HostEntity.HostEntityBuilder makeHost() {
        return HostEntity.builder()
                .uuid(HOST_UUID)
                .url(HOST_URL)
                .alive(false)
                .heartbeatDuration(Duration.ZERO)
                .os(OperatingSystem.UNKNOWN);
    }

    @Test
    void testGetOrCreateHost_nonExistent() throws IOException {
        when(fileService.fileExists(any())).thenReturn(true);
        when(fileService.readFile(any())).thenReturn(UUID_FILE_CONTENTS);
        when(hostRepository.findByUuid(HOST_UUID)).thenReturn(Optional.empty());
        when(systemService.getOs()).thenReturn(OperatingSystem.LINUX);
        when(hostRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        localHostService.setUp();
        final HostEntity createdHost = localHostService.getOrCreateHost();

        verify(hostRepository).save(hostCaptor.capture());
        assertSame(hostCaptor.getValue(), createdHost);
        assertEquals(HOST_UUID, createdHost.getUuid());
        assertTrue(createdHost.isAlive());
        assertSame(OperatingSystem.LINUX, createdHost.getOs());
    }

    @Test
    void testHeartbeat() throws IOException {
        final Runnable heartbeat = prepareHeartbeat();
        when(hostRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(systemService.getOs()).thenReturn(OperatingSystem.UNKNOWN);

        heartbeat.run();
        verify(hostRepository).save(hostCaptor.capture());
        assertTrue(hostCaptor.getValue().isAlive());
        assertEquals(HOST_UUID, hostCaptor.getValue().getUuid());
        assertEquals(HOST_URL, hostCaptor.getValue().getUrl());
    }

    @Test
    void testGetOrCreateHost() throws IOException {
        when(fileService.fileExists(any())).thenReturn(true);
        when(fileService.readFile(any())).thenReturn(UUID_FILE_CONTENTS);
        final HostEntity host = mock(HostEntity.class);
        when(hostRepository.findByUuid(HOST_UUID)).thenReturn(Optional.of(host));

        localHostService.setUp();
        assertSame(host, localHostService.getOrCreateHost());
        verify(hostRepository).findByUuid(HOST_UUID);
    }

    @Test
    void testPreDestroy() throws IOException {
        when(fileService.fileExists(any())).thenReturn(true);
        when(fileService.readFile(any())).thenReturn(UUID_FILE_CONTENTS);
        when(hostRepository.findByUuid(eq(HOST_UUID))).thenReturn(Optional.of(HostEntity.builder()
                .uuid(HOST_UUID)
                .os(OperatingSystem.UNKNOWN)
                .alive(true)
                .heartbeatDuration(Duration.ofDays(1000)).lastHeartbeat(Instant.MAX).build()));
        when(asyncService.scheduleAtFixedRate(any(), any(), any())).thenAnswer(invocation -> future);

        localHostService.setUp();
        localHostService.preDestroy();
        verify(future, only()).cancel(false);
        verify(hostRepository).save(hostCaptor.capture());
        assertFalse(hostCaptor.getValue().isAlive());
    }

    private Runnable prepareHeartbeat() throws IOException {
        when(fileService.fileExists(any())).thenReturn(true);
        when(fileService.readFile(any())).thenReturn(UUID_FILE_CONTENTS);
        when(hostRepository.findByUuid(HOST_UUID)).thenReturn(Optional.of(makeHost().build()));
        when(asyncService.scheduleAtFixedRate(any(), any(), any())).thenAnswer(invocation -> future);

        localHostService.setUp();
        verify(asyncService, only()).scheduleAtFixedRate(heartbeatCaptor.capture(), any(), any());
        return heartbeatCaptor.getValue();
    }
}