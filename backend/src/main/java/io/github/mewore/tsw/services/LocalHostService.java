package io.github.mewore.tsw.services;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.repositories.HostRepository;
import io.github.mewore.tsw.services.util.AsyncService;
import io.github.mewore.tsw.services.util.FileService;
import io.github.mewore.tsw.services.util.SystemService;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Keeps and updates the information regarding the host where this Spring app is running. Unlike other hosts, this one
 * can be managed much more easily.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Service
public class LocalHostService {

    private static final Duration HEARTBEAT_DURATION = Duration.ofMinutes(1);

    private static final Logger LOGGER = LogManager.getLogger(LocalHostService.class);

    private static final Path UUID_FILE_PATH = Path.of(".tsw_host_uuid");

    private final HostRepository hostRepository;

    private final FileService fileService;

    private final AsyncService asyncService;

    private final SystemService systemService;

    @Getter
    private UUID hostUuid;

    private Future<?> heartbeatFuture;

    @PostConstruct
    void setUp() throws IOException {
        final UUID existingUuid = getUuid(fileService);
        if (existingUuid == null) {
            hostUuid = makeNewUuid();
            createHost();
        } else {
            hostUuid = existingUuid;
            doHeartbeat();
        }
        heartbeatFuture = asyncService.scheduleAtFixedRate(this::doHeartbeat, HEARTBEAT_DURATION, HEARTBEAT_DURATION);
    }

    public @NonNull HostEntity getOrCreateHost() {
        return findHost().orElseGet(this::createHost);
    }

    private Optional<HostEntity> findHost() {
        return hostRepository.findByUuid(hostUuid);
    }

    private @NonNull HostEntity createHost() {
        final HostEntity newHost = HostEntity.builder()
                .uuid(hostUuid)
                .heartbeatDuration(HEARTBEAT_DURATION)
                .os(systemService.getOs())
                .build();
        return hostRepository.save(newHost);
    }

    /**
     * Let the other hosts know that this host is alive.
     */
    private void doHeartbeat() {
        LOGGER.debug("Performing a heartbeat...");
        findHost().ifPresentOrElse(this::refreshHost, this::createHost);
        LOGGER.debug("Heartbeat done.");
    }

    private void refreshHost(final @NonNull HostEntity host) {
        host.setAlive(true);
        host.setHeartbeatDuration(HEARTBEAT_DURATION);
        host.setLastHeartbeat(Instant.now());
        host.setOs(systemService.getOs());
        hostRepository.save(host);
    }

    private static @Nullable UUID getUuid(final FileService fileService) throws IOException {
        if (!fileService.exists(UUID_FILE_PATH)) {
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

    private @NonNull UUID makeNewUuid() throws IOException {
        final UUID uuid = UUID.randomUUID();
        fileService.makeFile(UUID_FILE_PATH, uuid.toString());
        return uuid;
    }

    @PreDestroy
    public void preDestroy() {
        heartbeatFuture.cancel(false);
        findHost().ifPresent(host -> {
            host.setAlive(false);
            hostRepository.save(host);
        });
    }
}
