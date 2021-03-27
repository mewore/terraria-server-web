package io.github.mewore.tsw.models.terraria;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.hibernate.annotations.DynamicUpdate;

import io.github.mewore.tsw.models.HostEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import lombok.With;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder(access = AccessLevel.PRIVATE)
@Getter
@With
@ToString
@EqualsAndHashCode
@Entity
@Table(name = "terraria_world", uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "host_id"})})
@DynamicUpdate
public class TerrariaWorldEntity {

    @Builder.Default
    @Id
    @GeneratedValue
    private final Long id = null;

    @Builder.Default
    @Column(nullable = false)
    private final @NonNull String name = "";

    @Builder.Default
    @Basic(fetch = FetchType.LAZY)
    @Column
    private final Instant lastModified = Instant.now();

    @Builder.Default
    @JsonIgnore
    @Lob
    @Basic(fetch = FetchType.LAZY)
    private final byte[] data = new byte[0];

    @Builder.Default
    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private final @NonNull HostEntity host = HostEntity.builder().build();
}
