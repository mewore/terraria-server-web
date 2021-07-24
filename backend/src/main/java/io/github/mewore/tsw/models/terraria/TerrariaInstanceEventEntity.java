package io.github.mewore.tsw.models.terraria;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.hibernate.annotations.Immutable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@Entity
@Table(name = "terraria_instance_event")
@Immutable
public class TerrariaInstanceEventEntity {

    @JsonIgnore
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private @NonNull TerrariaInstanceEventType type;

    @Builder.Default
    @Column(nullable = false)
    private @NonNull Instant timestamp = Instant.now();

    @Builder.Default
    @Lob
    @Column(nullable = false)
    private @NonNull String text = "";

    @JsonIgnore
    @ManyToOne(optional = false)
    private @NonNull TerrariaInstanceEntity instance;
}
