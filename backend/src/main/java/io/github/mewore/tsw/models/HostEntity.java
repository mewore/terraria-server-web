package io.github.mewore.tsw.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.DynamicUpdate;
import org.springframework.lang.Nullable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@Getter
@Entity
@Table(name = "host")
@DynamicUpdate
public class HostEntity {

    private static final Path DEFAULT_TERRARIA_PATH =
            Path.of(System.getProperty("user.home"), ".local", "share", "Terraria", "Instances");

    private static final int DEFAULT_PORT = 8080;

    @Builder.Default
    @Id
    @GeneratedValue
    private final Long id = null;

    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    private final @NonNull UUID uuid = UUID.randomUUID();

    @Builder.Default
    @Getter(AccessLevel.NONE)
    @Column(nullable = false)
    private final boolean alive = true;

    @Builder.Default
    @Getter(AccessLevel.NONE)
    @Column(nullable = false)
    private final @NonNull Instant lastHeartbeat = Instant.now();

    @Builder.Default
    @Getter(AccessLevel.NONE)
    @Column(nullable = false)
    private final @NonNull Duration heartbeatDuration = Duration.ZERO;

    @Nullable
    @Builder.Default
    @Column
    private final String name = null;

    @Nullable
    @Builder.Default
    @Column
    private final String url = null;

    @Builder.Default
    @Column(nullable = false)
    private final @NonNull Path terrariaInstanceDirectory = DEFAULT_TERRARIA_PATH;

    public boolean isAlive() {
        return alive && lastHeartbeat.plus(heartbeatDuration.multipliedBy(2)).isAfter(Instant.now());
    }
}
