package io.github.mewore.tsw.models.terraria;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.annotations.DynamicUpdate;

import io.github.mewore.tsw.models.HostEntity;
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
@Table(name = "terraria_instance", uniqueConstraints = {@UniqueConstraint(columnNames = {"host_id", "uuid"})})
@DynamicUpdate
public class TerrariaInstanceEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private @NonNull UUID uuid = UUID.randomUUID();

    @Column
    private @Nullable String terrariaVersion;

    @Column
    private @Nullable String modLoaderVersion;

    @Column(length = 1023)
    private @Nullable String modLoaderReleaseUrl;

    @Column(length = 1023)
    private @Nullable String modLoaderArchiveUrl;

    @Column(length = 1023)
    private @Nullable String error;

    @Column
    @Enumerated(EnumType.STRING)
    private @Nullable TerrariaInstanceAction pendingAction;

    @Column
    private @Nullable Instant actionExecutionStartTime;

    @Column(nullable = false, length = 1023)
    private @NonNull Path location;

    @Column(nullable = false)
    private @NonNull String name;

    @Column(nullable = false, length = 1023)
    private @NonNull String terrariaServerUrl;

    @Column(nullable = false)
    private @NonNull Long modLoaderReleaseId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private @NonNull TerrariaInstanceState state;

    @JsonIgnore
    @ManyToOne(optional = false)
    private @NonNull HostEntity host;
}
