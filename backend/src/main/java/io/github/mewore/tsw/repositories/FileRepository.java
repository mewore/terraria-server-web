package io.github.mewore.tsw.repositories;

import javax.transaction.Transactional;
import java.net.URL;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.Nullable;

import io.github.mewore.tsw.models.FileEntity;

@Transactional
public interface FileRepository extends JpaRepository<FileEntity, Long> {

    @Nullable
    FileEntity findBySourceUrl(final URL sourceUrl);
}
