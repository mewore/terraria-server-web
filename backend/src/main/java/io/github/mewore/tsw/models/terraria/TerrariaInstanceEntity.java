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

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.lang.Nullable;

import io.github.mewore.tsw.models.HostEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.With;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@With
@Getter
@Entity
@Table(name = "terraria_instance", uniqueConstraints = {@UniqueConstraint(columnNames = {"host_id", "uuid"})})
@DynamicInsert
@DynamicUpdate
public class TerrariaInstanceEntity {

    @Builder.Default
    @Id
    @GeneratedValue
    private final Long id = null;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private final @NonNull UUID uuid = UUID.randomUUID();

    @Builder.Default
    @Column
    @Nullable
    private final String terrariaVersion = null;

    @Builder.Default
    @Column
    @Nullable
    private final String modLoaderVersion = null;

    @Builder.Default
    @Column(length = 1023)
    @Nullable
    private final String modLoaderReleaseUrl = null;

    @Builder.Default
    @Column(length = 1023)
    @Nullable
    private final String modLoaderArchiveUrl = null;

    @Builder.Default
    @Column(length = 1023)
    @Nullable
    private final String error = null;

    @Builder.Default
    @Column
    @Enumerated(EnumType.STRING)
    @Nullable
    private final TerrariaInstanceAction pendingAction = null;

    @Builder.Default
    @Column
    @Nullable
    private final Instant actionExecutionStartTime = null;

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
