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

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@With
@Getter
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

    @Column(nullable = false)
    private @NonNull String name;

    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private @NonNull Instant lastModified;

    @JsonIgnore
    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] data;

    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private @NonNull HostEntity host;
}
