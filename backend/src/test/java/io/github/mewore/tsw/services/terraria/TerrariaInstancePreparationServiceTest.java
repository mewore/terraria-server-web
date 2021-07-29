package io.github.mewore.tsw.services.terraria;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import io.github.mewore.tsw.exceptions.InvalidInstanceException;
import io.github.mewore.tsw.exceptions.NotFoundException;
import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.file.OperatingSystem;
import io.github.mewore.tsw.models.github.GitHubRelease;
import io.github.mewore.tsw.models.github.GitHubReleaseAsset;
import io.github.mewore.tsw.models.terraria.TModLoaderVersionViewModel;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceAction;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceDefinitionModel;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;
import io.github.mewore.tsw.repositories.HostRepository;
import io.github.mewore.tsw.services.GithubService;
import io.github.mewore.tsw.services.util.FileService;
import io.github.mewore.tsw.services.util.HttpService;
import io.github.mewore.tsw.services.util.InputStreamSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerrariaInstancePreparationServiceTest {

    private static final GitHubReleaseAsset T_MOD_LOADER_ASSET_LINUX = makeTModLoaderAsset(
            "tModLoader.Linux.v0.11.8.1.zip");

    private static final GitHubReleaseAsset T_MOD_LOADER_ASSET_WINDOWS = makeTModLoaderAsset(
            "tModLoader.Windows.v0.11.8.1.zip");

    private static final GitHubReleaseAsset T_MOD_LOADER_ASSET_MAC = makeTModLoaderAsset(
            "tModLoader.Mac.v0.11.8.1.zip");

    private static final GitHubRelease T_MOD_LOADER_RELEASE = new GitHubRelease(
            Arrays.asList(T_MOD_LOADER_ASSET_LINUX, makeTModLoaderAsset("tModLoader.Linux.v0.11.8.1.tar.gz"),
                    T_MOD_LOADER_ASSET_MAC, T_MOD_LOADER_ASSET_WINDOWS,
                    GitHubReleaseAsset.builder().name("no-download-url").build()), 8, "v0.11.8.1", null,
            "tmodloader-release-url");

    private static final GitHubRelease EMPTY_RELEASE = new GitHubRelease(Collections.emptyList(), 8, "v0.11.8.1", null,
            null);

    private static final TerrariaInstanceDefinitionModel INSTANCE_CREATION_MODEL = new TerrariaInstanceDefinitionModel(
            "name", 8, "http://terraria.org/server/terraria-server-1003.zip");

    @InjectMocks
    private TerrariaInstancePreparationService terrariaInstancePreparationService;

    @Mock
    private GithubService githubService;

    @Mock
    private HttpService httpService;

    @Mock
    private FileService fileService;

    @Mock
    private HostRepository hostRepository;

    @Mock
    private TerrariaInstanceService terrariaInstanceService;

    @Captor
    private ArgumentCaptor<TerrariaInstanceEntity> instanceCaptor;

    @Captor
    private ArgumentCaptor<InputStreamSupplier> inputStreamSupplierCaptor;

    @Captor
    private ArgumentCaptor<Pattern> patternCaptor;

    private static TerrariaInstanceEntity.TerrariaInstanceEntityBuilder makeInstance() {
        return TerrariaInstanceFactory.makeInstanceBuilder()
                .error("previous error")
                .terrariaServerUrl("http://terraria.org/server/terraria-server-1003.zip")
                .modLoaderReleaseId(8L)
                .state(TerrariaInstanceState.DEFINED);
    }

    private static HostEntity makeHostWithOs(final OperatingSystem os) {
        final HostEntity host = mock(HostEntity.class);
        when(host.getOs()).thenReturn(os);
        return host;
    }

    private static GitHubReleaseAsset makeTModLoaderAsset(final String fileName) {
        return GitHubReleaseAsset.builder()
                .name(fileName)
                .browserDownloadUrl("file://tModLoader/v0.11.8.1/" + fileName)
                .build();
    }

    @Test
    void testFetchTModLoaderVersions() throws IOException {
        when(githubService.getReleases("tModLoader", "tModLoader")).thenReturn(
                Collections.singletonList(T_MOD_LOADER_RELEASE));

        final List<TModLoaderVersionViewModel> versions = terrariaInstancePreparationService.fetchTModLoaderVersions();
        verify(githubService).getReleases("tModLoader", "tModLoader");
        assertEquals(1, versions.size());
        assertEquals(8, versions.get(0).getReleaseId());
        assertEquals("v0.11.8.1", versions.get(0).getVersion());
    }

    @Test
    void testDefineTerrariaInstance() throws NotFoundException {
        final HostEntity host = mock(HostEntity.class);
        when(host.getTerrariaInstanceDirectory()).thenReturn(Path.of("/"));
        when(hostRepository.findById(1L)).thenReturn(Optional.of(host));

        when(terrariaInstanceService.saveInstance(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final TerrariaInstanceEntity createdInstance = terrariaInstancePreparationService.defineTerrariaInstance(1,
                INSTANCE_CREATION_MODEL);
        assertEquals("/" + createdInstance.getUuid(), createdInstance.getLocation().toString());
        assertSame(INSTANCE_CREATION_MODEL.getInstanceName(), createdInstance.getName());
        assertSame(INSTANCE_CREATION_MODEL.getTerrariaServerArchiveUrl(), createdInstance.getTerrariaServerUrl());
        assertSame(INSTANCE_CREATION_MODEL.getModLoaderReleaseId(), createdInstance.getModLoaderReleaseId());
        assertSame(TerrariaInstanceState.DEFINED, createdInstance.getState());
        assertSame(TerrariaInstanceAction.SET_UP, createdInstance.getPendingAction());
        assertSame(host, createdInstance.getHost());
    }

    @Test
    void testDefineTerrariaInstance_noHost() {
        when(hostRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> terrariaInstancePreparationService.defineTerrariaInstance(1, INSTANCE_CREATION_MODEL));
    }

    @Test
    void testSetUpTerrariaInstance() throws IOException, NotFoundException, InvalidInstanceException {
        final TerrariaInstanceEntity instance = makeInstance().host(makeHostWithOs(OperatingSystem.LINUX)).build();

        when(githubService.getRelease("tModLoader", "tModLoader", 8L)).thenReturn(T_MOD_LOADER_RELEASE);

        final Path serverZipCacheLocation = Path.of("terraria-servers", "terraria-server-1003.zip");
        when(fileService.hasFileInCache(serverZipCacheLocation)).thenReturn(true);

        when(terrariaInstanceService.saveInstance(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final File newInstanceDirectory = new File("instance-dir");
        when(fileService.reserveDirectory(any())).thenReturn(newInstanceDirectory);

        final InputStream serverZipStream = new ByteArrayInputStream(new byte[0]);
        when(fileService.cache(eq(serverZipCacheLocation), inputStreamSupplierCaptor.capture(), isNull())).thenReturn(
                serverZipStream);

        final InputStream tModLoaderStream = new ByteArrayInputStream(new byte[0]);
        when(githubService.fetchAsset(T_MOD_LOADER_ASSET_LINUX)).thenReturn(tModLoaderStream);

        when(fileService.makeFilesInDirectoryExecutable(same(newInstanceDirectory), any())).thenReturn(true);


        final TerrariaInstanceEntity preparedInstance = terrariaInstancePreparationService.setUpInstance(instance);

        verify(fileService).unzip(serverZipStream, newInstanceDirectory, "1003" + File.separator + "Linux");
        verify(fileService).unzip(tModLoaderStream, newInstanceDirectory);

        verify(httpService, never()).requestAsStream(any());
        final InputStream serverHttpStream = new ByteArrayInputStream(new byte[0]);
        final URL terrariaServerUrl = new URL("http://terraria.org/server/terraria-server-1003.zip");
        when(httpService.requestAsStream(terrariaServerUrl)).thenReturn(serverHttpStream);
        final InputStream suppliedServerStream = inputStreamSupplierCaptor.getValue().supplyStream();
        verify(httpService).requestAsStream(terrariaServerUrl);
        assertSame(serverHttpStream, suppliedServerStream);

        verify(fileService).makeFilesInDirectoryExecutable(any(), patternCaptor.capture());
        assertEquals("^tModLoaderServer.*", patternCaptor.getValue().toString());

        verify(terrariaInstanceService, times(2)).saveInstance(instanceCaptor.capture());

        assertSame(preparedInstance, instanceCaptor.getAllValues().get(0));
        assertSame(preparedInstance, instanceCaptor.getAllValues().get(1));

        assertSame(instance.getHost(), preparedInstance.getHost());
        assertEquals(newInstanceDirectory.toPath(), preparedInstance.getLocation());
        assertSame(instance.getName(), preparedInstance.getName());
        assertEquals("1.0.0.3", preparedInstance.getTerrariaVersion());
        assertSame(instance.getTerrariaServerUrl(), preparedInstance.getTerrariaServerUrl());
        assertEquals("0.11.8.1", preparedInstance.getModLoaderVersion());
        assertSame(T_MOD_LOADER_ASSET_LINUX.getBrowserDownloadUrl(), preparedInstance.getModLoaderArchiveUrl());
        assertEquals("tmodloader-release-url", preparedInstance.getModLoaderReleaseUrl());
        assertSame(TerrariaInstanceState.IDLE, preparedInstance.getState());
        assertNull(preparedInstance.getPendingAction());
    }

    @Test
    void testSetUpTerrariaInstance_Windows() throws IOException, NotFoundException, InvalidInstanceException {
        when(githubService.getRelease(any(), any(), anyLong())).thenReturn(T_MOD_LOADER_RELEASE);
        when(terrariaInstanceService.saveInstance(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(fileService.reserveDirectory(any())).thenReturn(new File("instance-dir"));
        when(fileService.cache(any(), any(), any())).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(githubService.fetchAsset(T_MOD_LOADER_ASSET_WINDOWS)).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(fileService.makeFilesInDirectoryExecutable(any(), any())).thenReturn(true);

        terrariaInstancePreparationService.setUpInstance(
                makeInstance().host(makeHostWithOs(OperatingSystem.WINDOWS)).build());
        verify(fileService).unzip(any(), any(), eq("1003" + File.separator + "Windows"));
    }

    @Test
    void testSetUpTerrariaInstance_Mac() throws IOException, NotFoundException, InvalidInstanceException {
        when(githubService.getRelease(any(), any(), anyLong())).thenReturn(T_MOD_LOADER_RELEASE);
        when(terrariaInstanceService.saveInstance(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(fileService.reserveDirectory(any())).thenReturn(new File("instance-dir"));
        when(fileService.cache(any(), any(), any())).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(githubService.fetchAsset(T_MOD_LOADER_ASSET_MAC)).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(fileService.makeFilesInDirectoryExecutable(any(), any())).thenReturn(true);

        terrariaInstancePreparationService.setUpInstance(
                makeInstance().host(makeHostWithOs(OperatingSystem.MAC)).build());
        verify(fileService).unzip(any(), any(), eq("1003" + File.separator + "Mac"));
    }

    @Test
    void testSetUpTerrariaInstance_invalidServerUrl() {
        final TerrariaInstanceEntity instance = makeInstance().terrariaServerUrl("file://invalid-url").build();
        final Exception exception = assertThrows(InvalidInstanceException.class,
                () -> terrariaInstancePreparationService.setUpInstance(instance));
        assertEquals("The URL file://invalid-url does not match the regular expression " +
                        "^https?://(www\\.)?terraria\\.org/[^?]+/(terraria-server-(\\d+).zip)(\\?\\d+)?$",
                exception.getMessage());
    }

    @Test
    void testSetUpTerrariaInstance_unsupportedOs() {
        final TerrariaInstanceEntity instance = makeInstance().host(makeHostWithOs(OperatingSystem.SOLARIS)).build();
        final Exception exception = assertThrows(InvalidInstanceException.class,
                () -> terrariaInstancePreparationService.setUpInstance(instance));
        assertEquals("TModLoader is not supported for the [SOLARIS] operating system", exception.getMessage());
    }

    @Test
    void testSetUpTerrariaInstance_noSuitableModLoaderAsset() throws IOException, NotFoundException {
        when(githubService.getRelease("tModLoader", "tModLoader", 8L)).thenReturn(EMPTY_RELEASE);

        final TerrariaInstanceEntity instance = makeInstance().host(makeHostWithOs(OperatingSystem.LINUX)).build();
        final Exception exception = assertThrows(InvalidInstanceException.class,
                () -> terrariaInstancePreparationService.setUpInstance(instance));
        assertEquals("Could not find a Linux archive of the TModLoader release 8", exception.getMessage());
    }

    @Test
    void testSetUpTerrariaInstance_unreachableServerUrl() throws IOException, NotFoundException {
        when(fileService.hasFileInCache(any())).thenReturn(false);
        when(githubService.getRelease(any(), any(), anyLong())).thenReturn(T_MOD_LOADER_RELEASE);
        doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found")).when(httpService)
                .checkUrl(eq(new URL("http://terraria.org/server/terraria-server-1003.zip")));

        final TerrariaInstanceEntity instance = makeInstance().host(makeHostWithOs(OperatingSystem.LINUX)).build();
        final Exception exception = assertThrows(InvalidInstanceException.class,
                () -> terrariaInstancePreparationService.setUpInstance(instance));
        assertEquals("The response at URL http://terraria.org/server/terraria-server-1003.zip is " +
                "HTTP code 404: Not Found", exception.getMessage());
    }

    @Test
    void testSetUpTerrariaInstance_noModLoaderServerFiles() throws IOException, NotFoundException {
        when(githubService.getRelease(any(), any(), anyLong())).thenReturn(T_MOD_LOADER_RELEASE);
        when(terrariaInstanceService.saveInstance(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(fileService.reserveDirectory(any())).thenReturn(mock(File.class));
        when(fileService.cache(any(), any(), any())).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(githubService.fetchAsset(any())).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(fileService.makeFilesInDirectoryExecutable(any(), any())).thenReturn(false);

        final Exception exception = assertThrows(RuntimeException.class,
                () -> terrariaInstancePreparationService.setUpInstance(
                        makeInstance().host(makeHostWithOs(OperatingSystem.LINUX)).build()));
        assertEquals("Failed to find any files, the name of which matches /^tModLoaderServer.*/",
                exception.getMessage());
    }
}