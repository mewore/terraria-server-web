package io.github.mewore.tsw.services;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.repositories.HostRepository;
import io.github.mewore.tsw.services.util.AsyncService;
import io.github.mewore.tsw.services.util.FileService;
import lombok.Getter;
import lombok.NonNull;

/**
 * Keeps and updates the information regarding the host where this Spring app is running. Unlike other hosts, this one
 * can be managed much more easily.
 */
@Service
public class LocalHostService {

    private static final Duration HEARTBEAT_DURATION = Duration.ofMinutes(1);

    private static final Logger LOGGER = LogManager.getLogger(LocalHostService.class);

    private static final Path UUID_FILE_PATH = Paths.get(".tsw_host_uuid");

    @Getter
    private final UUID hostUuid;

    private final @NonNull HostRepository hostRepository;

    private final @NonNull Future<?> heartbeatFuture;

    public LocalHostService(final @NonNull HostRepository hostRepository,
            final @NonNull FileService fileService,
            final @NonNull AsyncService asyncService) throws IOException {

        this.hostRepository = hostRepository;

        final UUID existingUuid = getUuid(fileService);
        hostUuid = existingUuid != null ? existingUuid : makeNewUuid(fileService);

        heartbeatFuture = asyncService.scheduleAtFixedRate(this::doHeartbeat, Duration.ZERO, HEARTBEAT_DURATION);
    }

    public @NonNull HostEntity getHost() {
        return hostRepository.findByUuid(hostUuid)
                .orElseGet(() -> hostRepository.save(
                        HostEntity.builder().uuid(hostUuid).heartbeatDuration(HEARTBEAT_DURATION).build()));
    }

    /**
     * Let the other hosts know that this host is alive.
     */
    private void doHeartbeat() {
        LOGGER.debug("Heartbeat...");
        hostRepository.save(getHost().toBuilder()
                .alive(true)
                .heartbeatDuration(HEARTBEAT_DURATION)
                .lastHeartbeat(Instant.now())
                .build());
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
        hostRepository.save(getHost().toBuilder().alive(false).build());
    }
}
