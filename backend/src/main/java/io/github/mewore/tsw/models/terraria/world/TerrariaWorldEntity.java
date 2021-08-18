package io.github.mewore.tsw.models.terraria.world;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Positive;
import java.time.Instant;
import java.util.Set;

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
@Table(name = "terraria_world", uniqueConstraints = {@UniqueConstraint(name = "terraria_world_file_name_host_id_ukey"
        , columnNames = {"file_name", "host_id"})})
@DynamicUpdate
public class TerrariaWorldEntity {

    @Setter(AccessLevel.NONE)
    @Id
    @GeneratedValue
    private Long id;

    @JsonIgnore
    @Column(nullable = false, name = "file_name")
    private @NonNull String fileName;

    @Column(nullable = false)
    private @NonNull String displayName;

    @Column
    private @Nullable Instant lastModified;

    @Column
    private @Positive @Nullable Set<String> mods;

    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private @NonNull HostEntity host;

    @Column
    @Enumerated(EnumType.STRING)
    private @Nullable WorldSizeOption size;

    @Column
    @Enumerated(EnumType.STRING)
    private @Nullable WorldDifficultyOption difficulty;
}
