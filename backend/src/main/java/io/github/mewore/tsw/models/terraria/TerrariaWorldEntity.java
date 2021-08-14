package io.github.mewore.tsw.models.terraria;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
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
@Table(name = "terraria_world", uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "host_id"})})
@DynamicUpdate
public class TerrariaWorldEntity {

    @Setter(AccessLevel.NONE)
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private @NonNull String name;

    @Column(nullable = false)
    private @NonNull Instant lastModified;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false, orphanRemoval = true)
    @MapsId
    @JoinColumn(name = "id")
    private @NonNull TerrariaWorldFileEntity file;

    @Column
    private @Positive @Nullable Set<String> mods;

    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private @NonNull HostEntity host;

    public TerrariaWorldFileEntity updateFile(final TerrariaWorldFileEntity newFile) {
        file.setContent(newFile.getContent());
        file.setName(newFile.getName());
        return file;
    }
}
