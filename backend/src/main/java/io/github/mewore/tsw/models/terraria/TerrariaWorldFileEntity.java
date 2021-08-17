package io.github.mewore.tsw.models.terraria;


import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.checkerframework.common.returnsreceiver.qual.This;

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
@Table(name = "terraria_world_file")
public class TerrariaWorldFileEntity {

    @Id
    private Long worldId;

    @OneToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @MapsId
    private @NonNull TerrariaWorldEntity world;

    @Column(nullable = false)
    private @NonNull String name;

    @Lob
    @Basic(optional = false)
    private byte @NonNull [] content;

    public @This TerrariaWorldFileEntity update(final TerrariaWorldFileEntity newFile,
            final TerrariaWorldEntity newWorld) {
        content = newFile.getContent();
        name = newFile.getName();
        world = newWorld;
        return this;
    }
}
