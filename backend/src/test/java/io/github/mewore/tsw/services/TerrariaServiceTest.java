package io.github.mewore.tsw.services;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mewore.tsw.models.github.GitHubRelease;
import io.github.mewore.tsw.models.terraria.TModLoaderVersionViewModel;
import io.github.mewore.tsw.services.util.HttpService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerrariaServiceTest {

    private static final List<GitHubRelease> RELEASES =
            Collections.singletonList(new GitHubRelease(Collections.emptyList(), 18, "v1.0.0", "release-1-url"));

    @InjectMocks
    private TerrariaService terrariaService;

    @Mock
    private HttpService httpService;

    @Test
    void testFetchTModLoaderVersions() throws IOException {
        when(httpService.get(any(), any())).thenReturn(RELEASES);

        final List<TModLoaderVersionViewModel> versions = terrariaService.fetchTModLoaderVersions();
        verify(httpService).get(any(), any());
        assertEquals(1, versions.size());
        assertEquals(18, versions.get(0).getReleaseId());
        assertEquals("v1.0.0", versions.get(0).getVersion());
    }

    @Test
    void testFetchTModLoaderVersions_calledTwice() throws IOException {
        when(httpService.get(any(), any())).thenReturn(RELEASES);

        terrariaService.fetchTModLoaderVersions();
        verify(httpService).get(any(), any());

        // NOTE: If there is a delay between the two calls, this will fail! Bad test design, but it will do for now.
        terrariaService.fetchTModLoaderVersions();
        verify(httpService, atMostOnce()).get(any(), any());
    }
}