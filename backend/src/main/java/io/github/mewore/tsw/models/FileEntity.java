package io.github.mewore.tsw.models;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.net.URL;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.github.mewore.tsw.models.file.FileOs;
import io.github.mewore.tsw.models.file.FileType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
@Entity
@Table(name = "file")
public class FileEntity {

    @Builder.Default
    @Id
    @GeneratedValue
    private final Long id = null;

    @Builder.Default
    @Column(nullable = false, unique = true)
    private final URL sourceUrl = null;

    @Builder.Default
    @Lob
    @Basic(fetch = FetchType.LAZY, optional = false)
    @Column(nullable = false)
    private final byte[] data = new byte[0];

    @JsonIgnore
    @Builder.Default
    @Column(nullable = false)
    private final String version = "1";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private final FileType type = FileType.UNKNOWN;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private final FileOs os = FileOs.ANY;
}
