package io.github.mewore.tsw.services;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mewore.tsw.exceptions.IncorrectUrlException;
import io.github.mewore.tsw.exceptions.NotFoundException;
import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.file.OperatingSystem;
import io.github.mewore.tsw.models.github.GitHubRelease;
import io.github.mewore.tsw.models.github.GitHubReleaseAsset;
import io.github.mewore.tsw.models.terraria.TModLoaderVersionViewModel;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceCreationModel;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;
import io.github.mewore.tsw.models.terraria.TerrariaWorldEntity;
import io.github.mewore.tsw.repositories.TerrariaInstanceRepository;
import io.github.mewore.tsw.repositories.terraria.TerrariaWorldRepository;
import io.github.mewore.tsw.services.util.FileService;
import io.github.mewore.tsw.services.util.HttpService;
import io.github.mewore.tsw.services.util.InputStreamSupplier;
import io.github.mewore.tsw.services.util.SystemService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerrariaServiceTest {

    private static final GitHubReleaseAsset T_MOD_LOADER_ASSET_LINUX =
            makeTModLoaderAsset("tModLoader.Linux.v0.11.8.1.zip");

    private static final GitHubReleaseAsset T_MOD_LOADER_ASSET_WINDOWS =
            makeTModLoaderAsset("tModLoader.Windows.v0.11.8.1.zip");

    private static final GitHubRelease T_MOD_LOADER_RELEASE = new GitHubRelease(
            Arrays.asList(T_MOD_LOADER_ASSET_LINUX, makeTModLoaderAsset("tModLoader.Linux.v0.11.8.1.tar.gz"),
                    T_MOD_LOADER_ASSET_WINDOWS, GitHubReleaseAsset.builder().name("no-download-url").build()), 8,
            "v0.11.8.1", null);

    private static final TerrariaInstanceCreationModel INSTANCE_CREATION_MODEL =
            new TerrariaInstanceCreationModel("name", 1, 8, "http://terraria.org/server/terraria-server-1333.zip");

    private static final File TERRARIA_WORLD_DIRECTORY =
            Path.of(System.getProperty("user.home"), ".local", "share", "Terraria", "ModLoader", "Worlds").toFile();

    @InjectMocks
    private TerrariaService terrariaService;

    @Mock
    private LocalHostService localHostService;

    @Mock
    private GithubService githubService;

    @Mock
    private HttpService httpService;

    @Mock
    private FileService fileService;

    @Mock
    private SystemService systemService;

    @Mock
    private TerrariaWorldRepository terrariaWorldRepository;

    @Mock
    private TerrariaInstanceRepository terrariaInstanceRepository;

    @Captor
    private ArgumentCaptor<InputStreamSupplier> inputStreamSupplierCaptor;

    @Captor
    private ArgumentCaptor<List<TerrariaWorldEntity>> terrariaWorldListCaptor;

    @Test
    void testSetUp() throws IOException {
        final HostEntity host = HostEntity.builder().build();
        when(localHostService.getHost()).thenReturn(host);

        final File wldFile = Mockito.spy(new File("world.wld"));
        when(wldFile.lastModified()).thenReturn(1L);
        when(fileService.listFiles(TERRARIA_WORLD_DIRECTORY, "wld")).thenReturn(
                new File[]{new File("world-without-twld.wld"), wldFile});

        final File twldFile = Mockito.spy(new File("world.twld"));
        when(twldFile.lastModified()).thenReturn(8L);
        when(fileService.listFiles(TERRARIA_WORLD_DIRECTORY, "twld")).thenReturn(
                new File[]{new File("world-without-wld.twld"), twldFile});

        final byte[] zipData = new byte[]{1, 2, 3};
        when(fileService.zip(wldFile, twldFile)).thenReturn(zipData);

        terrariaService.setUp();
        verify(terrariaWorldRepository).setHostWorlds(same(host), terrariaWorldListCaptor.capture());

        final TerrariaWorldEntity expectedSavedWorld =
                new TerrariaWorldEntity(null, "world", Instant.ofEpochMilli(8), zipData, host);
        assertEquals(Collections.singletonList(expectedSavedWorld), terrariaWorldListCaptor.getValue());
    }

    @Test
    void testFetchTModLoaderVersions() throws IOException {
        when(githubService.getReleases("tModLoader", "tModLoader")).thenReturn(
                Collections.singletonList(T_MOD_LOADER_RELEASE));

        final List<TModLoaderVersionViewModel> versions = terrariaService.fetchTModLoaderVersions();
        verify(githubService).getReleases("tModLoader", "tModLoader");
        assertEquals(1, versions.size());
        assertEquals(8, versions.get(0).getReleaseId());
        assertEquals("v0.11.8.1", versions.get(0).getVersion());
    }

    @Test
    void testCreateTerrariaInstance_invalidTerrariaServerUrl() {
        assertThrows(IncorrectUrlException.class, () -> terrariaService.createTerrariaInstance(
                new TerrariaInstanceCreationModel("Instance with invalid URL", 1, 1, "invalid URL")));
    }

    @Test
    void testCreateTerrariaInstance_noSuitableModLoaderAsset() throws IOException, NotFoundException {
        when(githubService.getRelease(any(), any(), anyLong())).thenReturn(T_MOD_LOADER_RELEASE);
        when(systemService.getOs()).thenReturn(OperatingSystem.MAC);

        assertThrows(NotFoundException.class, () -> terrariaService.createTerrariaInstance(INSTANCE_CREATION_MODEL));
    }

    @Test
    void testCreateTerrariaInstance_unsupportedOs() {
        when(systemService.getOs()).thenReturn(OperatingSystem.SOLARIS);
        assertThrows(UnsupportedOperationException.class,
                () -> terrariaService.createTerrariaInstance(INSTANCE_CREATION_MODEL));
    }

    @Test
    void testCreateTerrariaInstance() throws IOException, NotFoundException, IncorrectUrlException {
        when(systemService.getOs()).thenReturn(OperatingSystem.LINUX);
        when(githubService.getRelease("tModLoader", "tModLoader", 8L)).thenReturn(T_MOD_LOADER_RELEASE);

        final InputStream serverZipStream = new ByteArrayInputStream(new byte[0]);
        when(fileService.cache(eq(Path.of("terraria-servers", "terraria-server-1333.zip")),
                inputStreamSupplierCaptor.capture(), isNull())).thenReturn(serverZipStream);

        final File newInstanceDirectory = new File("new-instance-directory");

        final Path instanceParentDirectory = Path.of("instance-dir");
        final HostEntity localHost = HostEntity.builder().terrariaInstanceDirectory(instanceParentDirectory).build();
        when(localHostService.getHost()).thenReturn(localHost);
        when(fileService.reserveDirectory(any())).thenReturn(newInstanceDirectory);

        final InputStream tModLoaderStream = new ByteArrayInputStream(new byte[0]);
        when(githubService.fetchAsset(T_MOD_LOADER_ASSET_LINUX)).thenReturn(tModLoaderStream);

        when(terrariaInstanceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        final TerrariaInstanceEntity createdInstance = terrariaService.createTerrariaInstance(INSTANCE_CREATION_MODEL);
        verify(fileService).unzip(serverZipStream, newInstanceDirectory, "1333" + File.separator + "Linux");
        verify(fileService).unzip(tModLoaderStream, newInstanceDirectory);

        verify(httpService, never()).requestAsStream(any());
        final InputStream serverHttpStream = new ByteArrayInputStream(new byte[0]);
        final URL terrariaServerUrl = new URL("http://terraria.org/server/terraria-server-1333.zip");
        when(httpService.requestAsStream(terrariaServerUrl)).thenReturn(serverHttpStream);
        final InputStream suppliedServerStream = inputStreamSupplierCaptor.getValue().supplyStream();
        verify(httpService).requestAsStream(terrariaServerUrl);
        assertSame(serverHttpStream, suppliedServerStream);

        assertSame(localHost, createdInstance.getHost());
        assertEquals(newInstanceDirectory.toPath(), createdInstance.getLocation());
        assertSame(INSTANCE_CREATION_MODEL.getInstanceName(), createdInstance.getName());
        assertEquals("1.3.3.3", createdInstance.getTerrariaVersion());
        assertSame(INSTANCE_CREATION_MODEL.getTerrariaServerArchiveUrl(), createdInstance.getTerrariaServerUrl());
        assertEquals("0.11.8.1", createdInstance.getModLoaderVersion());
        assertSame(T_MOD_LOADER_ASSET_LINUX.getBrowserDownloadUrl(), createdInstance.getModLoaderUrl());
        assertSame(TerrariaInstanceState.STOPPED, createdInstance.getState());
    }

    @Test
    void testCreateTerrariaInstance_Windows() throws IOException, NotFoundException, IncorrectUrlException {
        when(systemService.getOs()).thenReturn(OperatingSystem.WINDOWS);
        when(githubService.getRelease("tModLoader", "tModLoader", 8L)).thenReturn(T_MOD_LOADER_RELEASE);

        when(fileService.cache(any(), any(), isNull())).thenReturn(new ByteArrayInputStream(new byte[0]));

        final HostEntity localHost = HostEntity.builder().terrariaInstanceDirectory(Path.of("instance-dir")).build();
        when(localHostService.getHost()).thenReturn(localHost);
        when(fileService.reserveDirectory(any())).thenReturn(new File("new-instance-directory"));
        when(githubService.fetchAsset(any())).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(terrariaInstanceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final TerrariaInstanceEntity createdInstance = terrariaService.createTerrariaInstance(INSTANCE_CREATION_MODEL);
        assertSame(T_MOD_LOADER_ASSET_WINDOWS.getBrowserDownloadUrl(), createdInstance.getModLoaderUrl());
    }

    private static GitHubReleaseAsset makeTModLoaderAsset(final String fileName) {
        return GitHubReleaseAsset
                .builder()
                .name(fileName)
                .browserDownloadUrl("file://tModLoader/v0.11.8.1/" + fileName)
                .build();
    }
}