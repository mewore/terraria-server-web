package io.github.mewore.tsw.services;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.repositories.HostRepository;
import io.github.mewore.tsw.services.util.AsyncService;
import io.github.mewore.tsw.services.util.FileService;
import lombok.NonNull;

/**
 * Keeps and updates the information regarding the host where this Spring app is running.
 */
@Service
public class LocalHostService {

    private static final Duration HEARTBEAT_DURATION = Duration.ofMinutes(1);

    private static final Logger LOGGER = LogManager.getLogger(LocalHostService.class);

    private static final Path UUID_FILE_PATH = Paths.get(".tsw_host_uuid");

    private final @NonNull AtomicReference<@NonNull HostEntity> hostReference;

    private final @NonNull HostRepository hostRepository;

    private final @NonNull Future<?> heartbeatFuture;

    public LocalHostService(final @NonNull HostRepository hostRepository,
            final FileService fileService,
            final AsyncService asyncService,
            @Nullable @Value("${tsw.host.url:#{null}}") final String url) throws IOException {

        this.hostRepository = hostRepository;

        hostReference = new AtomicReference<>(getInitialHost(fileService, hostRepository).toBuilder()
                .heartbeatDuration(HEARTBEAT_DURATION)
                .url(url)
                .build());
        heartbeatFuture = asyncService.scheduleAtFixedRate(this::doHeartbeat, Duration.ZERO, HEARTBEAT_DURATION);
    }

    private void doHeartbeat() {
        LOGGER.info("Heartbeat...");
        hostReference.updateAndGet(
                host -> hostRepository.save(host.toBuilder().alive(true).lastHeartbeat(Instant.now()).build()));
    }

    private static HostEntity getInitialHost(final FileService fileService, final HostRepository hostRepository)
            throws IOException {

        final UUID existingUuid = getUuid(fileService);
        return existingUuid == null
                ? HostEntity.builder().uuid(makeNewUuid(fileService)).build()
                : hostRepository.findByUuid(existingUuid).orElse(HostEntity.builder().uuid(existingUuid).build());
    }

    @Nullable
    private static UUID getUuid(final FileService fileService) throws IOException {
        if (!fileService.fileExists(UUID_FILE_PATH)) {
            return null;
        }

        final String uuidString = fileService.readFile(UUID_FILE_PATH).trim();
        try {
            return UUID.fromString(uuidString);
        } catch (final IllegalArgumentException e) {
            LOGGER.warn("Failed to parse the already saved UUID '{}' in file '{}'.", uuidString,
                    UUID_FILE_PATH.toAbsolutePath().toString());
            return null;
        }
    }

    private static @NonNull UUID makeNewUuid(final FileService fileService) throws IOException {
        final UUID uuid = UUID.randomUUID();
        fileService.makeFile(UUID_FILE_PATH, uuid.toString());
        return uuid;
    }

    @PreDestroy
    public void preDestroy() {
        heartbeatFuture.cancel(false);
        hostReference.updateAndGet(host -> hostRepository.save(host.toBuilder().alive(false).build()));
    }
}
