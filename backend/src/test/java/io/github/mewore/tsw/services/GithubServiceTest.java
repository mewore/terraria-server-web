package io.github.mewore.tsw.services;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mewore.tsw.exceptions.NotFoundException;
import io.github.mewore.tsw.models.github.GitHubRelease;
import io.github.mewore.tsw.models.github.GitHubReleaseAsset;
import io.github.mewore.tsw.services.util.FileService;
import io.github.mewore.tsw.services.util.HttpService;
import io.github.mewore.tsw.services.util.InputStreamSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GithubServiceTest {

    private static final GitHubRelease RELEASE_1 =
            new GitHubRelease(Collections.emptyList(), 1, "Release 1", "file://release-1-url", "http://release-1-html");

    private static final GitHubRelease RELEASE_2 =
            new GitHubRelease(Collections.emptyList(), 2, "Release 2", "file://release-2-url", "http://release-2-html");

    @InjectMocks
    private GithubService githubService;

    @Mock
    private HttpService httpService;

    @Mock
    private FileService fileService;

    @Captor
    private ArgumentCaptor<InputStreamSupplier> inputStreamSupplierCaptor;

    @Test
    void testGetReleases() throws IOException {
        final List<GitHubRelease> releases = Collections.singletonList(RELEASE_1);
        when(httpService.get(eq(new URL("https://api.github.com/repos/<author>/<repository>/releases")),
                any())).thenReturn(releases);

        final List<GitHubRelease> result = githubService.getReleases("<author>", "<repository>");
        assertEquals(releases, result);
        // Ensure that the returned releases are unmodifiable since they come from the cache where they are reused
        assertThrows(UnsupportedOperationException.class, result::clear);
    }

    @Test
    void testGetReleases_calledTwice() throws IOException {
        when(httpService.get(any(), any())).thenReturn(Collections.emptyList());

        githubService.getReleases("<author>", "<repository>");
        githubService.getReleases("<author>", "<repository>");
        verify(httpService).get(any(), any());
    }

    @Test
    void testGetRelease() throws IOException, NotFoundException {
        when(httpService.get(any(), any())).thenReturn(Arrays.asList(RELEASE_1, RELEASE_2));

        final GitHubRelease result = githubService.getRelease("<author>", "<repository>", 2);
        assertSame(RELEASE_2, result);
    }

    @Test
    void testGetRelease_notFound() throws IOException {
        when(httpService.get(any(), any())).thenReturn(Arrays.asList(RELEASE_1, RELEASE_2));
        assertThrows(NotFoundException.class, () -> githubService.getRelease("<author>", "<repository>", 3));
    }

    @Test
    void testFetchAsset() throws IOException {
        final GitHubReleaseAsset asset = GitHubReleaseAsset.builder().id(5).name("assetName").size(1024L).build();
        final InputStream cacheStream = new ByteArrayInputStream(new byte[0]);
        final Path cachePath = Path.of("github-assets", "5-assetName");
        when(fileService.cache(eq(cachePath), any(), eq(1024L))).thenReturn(cacheStream);

        final InputStream result = githubService.fetchAsset(asset);
        assertSame(cacheStream, result);
        verify(fileService).cache(eq(cachePath), inputStreamSupplierCaptor.capture(), eq(1024L));
        verify(httpService, never()).requestAsStream(any());
    }

    @Test
    void testFetchAsset_notInCache() throws IOException {
        final GitHubReleaseAsset asset =
                GitHubReleaseAsset.builder().id(5).name("name").browserDownloadUrl("file://asset-url").build();
        when(fileService.cache(any(), any(), any())).thenReturn(new ByteArrayInputStream(new byte[0]));
        githubService.fetchAsset(asset);

        verify(fileService).cache(any(), inputStreamSupplierCaptor.capture(), any());
        verify(httpService, never()).requestAsStream(any());

        final URL assetUrl = new URL("file://asset-url");
        final InputStream httpStream = new ByteArrayInputStream(new byte[0]);
        when(httpService.requestAsStream(assetUrl)).thenReturn(httpStream);

        final InputStream suppliedStream = inputStreamSupplierCaptor.getValue().supplyStream();
        verify(httpService).requestAsStream(assetUrl);
        assertSame(httpStream, suppliedStream);
    }

    @Test
    void testFetchAsset_notInCache_noUrl() throws IOException {
        final GitHubReleaseAsset asset = GitHubReleaseAsset.builder().id(5).name("name").build();
        when(fileService.cache(any(), any(), any())).thenReturn(new ByteArrayInputStream(new byte[0]));
        githubService.fetchAsset(asset);

        verify(fileService).cache(any(), inputStreamSupplierCaptor.capture(), any());
        final Exception exception =
                assertThrows(NullPointerException.class, () -> inputStreamSupplierCaptor.getValue().supplyStream());

        assertEquals(exception.getMessage(), "The asset with ID 5 does not have a download URL");
    }
}