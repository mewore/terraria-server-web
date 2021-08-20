package io.github.mewore.tsw.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicUpdate;

import io.github.mewore.tsw.models.file.OperatingSystem;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@Setter
@Entity
@Table(name = "host")
@DynamicUpdate
public class HostEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_TERRARIA_PATH = Path.of(System.getProperty("user.home"), ".local", "share",
            "Terraria", "Instances").toString();

    private static final int DEFAULT_PORT = 8080;

    @Setter(AccessLevel.NONE)
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private @NonNull UUID uuid;

    @Builder.Default
    @Getter(AccessLevel.NONE)
    @Column(nullable = false)
    private boolean alive = true;

    @JsonIgnore
    @Builder.Default
    @Getter(AccessLevel.NONE)
    @Column(nullable = false)
    private @NonNull Instant lastHeartbeat = Instant.now();

    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Column(nullable = false)
    private @NonNull Duration heartbeatDuration;

    @Column(nullable = false)
    @ColumnDefault("'UNKNOWN'")
    @Enumerated(EnumType.STRING)
    private @NonNull OperatingSystem os;

    @Column
    private @Nullable String name;

    @Column
    private @Nullable String url;

    @Builder.Default
    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private @NonNull String terrariaInstanceDirectory = DEFAULT_TERRARIA_PATH;

    public boolean isAlive() {
        return alive && lastHeartbeat.plus(heartbeatDuration.multipliedBy(2)).isAfter(Instant.now());
    }

    public Path getTerrariaInstanceDirectory() {
        return Path.of(terrariaInstanceDirectory);
    }
}
