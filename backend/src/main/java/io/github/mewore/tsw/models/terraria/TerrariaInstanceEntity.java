package io.github.mewore.tsw.models.terraria;


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
import java.nio.file.Path;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.springframework.lang.Nullable;

import io.github.mewore.tsw.models.HostEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import springfox.documentation.spring.web.paths.Paths;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor
@Builder(access = AccessLevel.PRIVATE)
@Getter
@Entity
@Table(name = "terraria_instance", uniqueConstraints = {@UniqueConstraint(columnNames = {"host_id", "uuid"})})
public class TerrariaInstanceEntity {

    @Builder.Default
    @Id
    @GeneratedValue
    private final Long id = null;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private final @NonNull UUID uuid = UUID.randomUUID();

    @Builder.Default
    @Column(nullable = false, length = 1023)
    private final @NonNull Path location = Path.of(Paths.ROOT);

    @Builder.Default
    @Column(nullable = false)
    private final @NonNull String name = "Unnamed";

    @Builder.Default
    @Column(nullable = false)
    private final @NonNull String terrariaVersion = "0";

    @Builder.Default
    @Column(nullable = false, length = 1023)
    private final @NonNull String terrariaServerUrl = "0";

    @Builder.Default
    @Column(nullable = false)
    private final @NonNull String modLoaderVersion = "0";

    @Builder.Default
    @Column(length = 1023)
    @Nullable
    private final String modLoaderReleaseUrl = "0";

    @Builder.Default
    @Column(nullable = false, length = 1023)
    private final @NonNull String modLoaderArchiveUrl = "0";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private final @NonNull TerrariaInstanceState state = TerrariaInstanceState.INVALID;

    @JsonIgnore
    @Builder.Default
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private final @NonNull HostEntity host = HostEntity.builder().build();
}
