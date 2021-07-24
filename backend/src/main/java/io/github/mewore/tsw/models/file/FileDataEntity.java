package io.github.mewore.tsw.models.file;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

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
@Table(name = "file_data")
@Immutable
public class FileDataEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private @NonNull String name;

    @Lob
    @Basic(optional = false)
    private byte @NonNull [] content;
}
