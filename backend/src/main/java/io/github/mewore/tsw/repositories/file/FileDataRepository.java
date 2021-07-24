package io.github.mewore.tsw.repositories.file;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.mewore.tsw.models.file.FileDataEntity;

@Transactional
public interface FileDataRepository extends JpaRepository<FileDataEntity, Long> {

}
