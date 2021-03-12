package io.github.mewore.tsw.services;

import java.io.IOException;
import java.net.URL;

import org.springframework.stereotype.Service;

import io.github.mewore.tsw.models.FileEntity;
import io.github.mewore.tsw.models.file.FileOs;
import io.github.mewore.tsw.models.file.FileType;
import io.github.mewore.tsw.repositories.FileRepository;
import io.github.mewore.tsw.services.util.HttpService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class DownloadService {

    private final FileRepository fileRepository;

    private final HttpService httpService;

    public FileEntity fetchOrPersistFile(final URL url,
            final String version,
            final FileType fileType,
            final FileOs fileOs) throws IOException {

        final FileEntity existingFile = fileRepository.findBySourceUrl(url);
        final FileEntity file = existingFile == null
                ? new FileEntity(null, url, httpService.requestRaw(url), version, fileType, fileOs)
                : new FileEntity(existingFile.getId(), url, existingFile.getData(), version, fileType, fileOs);

        return fileRepository.save(file);
    }
}
