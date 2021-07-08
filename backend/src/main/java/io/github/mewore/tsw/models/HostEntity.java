package io.github.mewore.tsw.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.lang.Nullable;

import io.github.mewore.tsw.models.file.OperatingSystem;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@Getter
@Entity
@Table(name = "host")
@DynamicInsert
@DynamicUpdate
public class HostEntity {

    private static final Path DEFAULT_TERRARIA_PATH =
            Path.of(System.getProperty("user.home"), ".local", "share", "Terraria", "Instances");

    private static final int DEFAULT_PORT = 8080;

    @Builder.Default
    @Id
    @GeneratedValue
    private final Long id = null;

    @Column(nullable = false, unique = true, updatable = false)
    private @NonNull UUID uuid;

    @Builder.Default
    @Getter(AccessLevel.NONE)
    @Column(nullable = false)
    private boolean alive = true;

    @Builder.Default
    @Getter(AccessLevel.NONE)
    @Column(nullable = false)
    private @NonNull Instant lastHeartbeat = Instant.now();

    @Getter(AccessLevel.NONE)
    @Column(nullable = false)
    private @NonNull Duration heartbeatDuration;

    @Column(nullable = false)
    @ColumnDefault("'UNKNOWN'")
    @Enumerated(EnumType.STRING)
    private @NonNull OperatingSystem os;

    @Nullable
    @Column
    private String name;

    @Nullable
    @Column
    private String url;

    @Builder.Default
    @Column(nullable = false)
    private @NonNull Path terrariaInstanceDirectory = DEFAULT_TERRARIA_PATH;

    public boolean isAlive() {
        return alive && lastHeartbeat.plus(heartbeatDuration.multipliedBy(2)).isAfter(Instant.now());
    }
}
