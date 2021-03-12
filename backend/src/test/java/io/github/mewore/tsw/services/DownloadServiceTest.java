package io.github.mewore.tsw.services;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mewore.tsw.models.FileEntity;
import io.github.mewore.tsw.models.file.FileOs;
import io.github.mewore.tsw.models.file.FileType;
import io.github.mewore.tsw.repositories.FileRepository;
import io.github.mewore.tsw.services.util.HttpService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DownloadServiceTest {

    private static final URL SOURCE_URL = getUrl();

    private static final byte[] FILE_BYTES = new byte[0];

    @InjectMocks
    private DownloadService downloadService;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private HttpService httpService;

    @Captor
    private ArgumentCaptor<FileEntity> fileCaptor;

    @BeforeEach
    void setUp() {
        when(fileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testDownloadAndPersistFile_existing() throws IOException {
        final FileEntity existingFile = Mockito.mock(FileEntity.class);
        when(existingFile.getData()).thenReturn(FILE_BYTES);
        when(existingFile.getId()).thenReturn(1L);
        when(fileRepository.findBySourceUrl(SOURCE_URL)).thenReturn(existingFile);

        final FileEntity returnedFile = downloadService.fetchOrPersistFile(SOURCE_URL, "1", FileType.UNKNOWN,
                FileOs.ANY);
        verify(httpService, never()).requestRaw(any());
        verify(fileRepository).save(fileCaptor.capture());
        assertSame(fileCaptor.getValue(), returnedFile);
        assertSame(SOURCE_URL, returnedFile.getSourceUrl());
        assertSame(FILE_BYTES, returnedFile.getData());
        assertEquals("1", returnedFile.getVersion());
        assertSame(FileType.UNKNOWN, returnedFile.getType());
        assertSame(FileOs.ANY, returnedFile.getOs());
    }

    @Test
    void testDownloadAndPersistFile_nonExistent() throws IOException {
        when(fileRepository.findBySourceUrl(SOURCE_URL)).thenReturn(null);
        when(httpService.requestRaw(SOURCE_URL)).thenReturn(FILE_BYTES);

        final FileEntity returnedFile = downloadService.fetchOrPersistFile(SOURCE_URL, "1", FileType.T_MOD_LOADER,
                FileOs.LINUX);
        verify(httpService).requestRaw(SOURCE_URL);
        verify(fileRepository).save(fileCaptor.capture());
        assertSame(fileCaptor.getValue(), returnedFile);
        assertSame(SOURCE_URL, returnedFile.getSourceUrl());
        assertSame(FILE_BYTES, returnedFile.getData());
        assertEquals("1", returnedFile.getVersion());
        assertSame(FileType.T_MOD_LOADER, returnedFile.getType());
        assertSame(FileOs.LINUX, returnedFile.getOs());
    }

    private static URL getUrl() {
        try {
            return new URL("http://test");
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException("The URL is invalid", e);
        }
    }
}