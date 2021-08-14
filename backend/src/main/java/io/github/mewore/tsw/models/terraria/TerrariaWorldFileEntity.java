package io.github.mewore.tsw.models.terraria;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

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

    @Setter(AccessLevel.NONE)
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private @NonNull String name;

    @Lob
    @Basic(optional = false)
    private byte @NonNull [] content;
}
