package io.github.mewore.tsw.config;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Future;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Component;

import io.github.mewore.tsw.models.file.OperatingSystem;
import io.github.mewore.tsw.services.util.AsyncService;
import io.github.mewore.tsw.services.util.FileService;
import io.github.mewore.tsw.services.util.HttpService;
import io.github.mewore.tsw.services.util.SystemService;
import io.github.mewore.tsw.services.util.process.TmuxService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestConfig {

    private static final Path WORLD_DIRECTORY = Path.of(System.getProperty("user.home"), ".local", "share", "Terraria",
            "ModLoader", "Worlds");

    @Bean
    AuthenticationProvider authenticationProvider() {
        return new TestAuthenticationProvider();
    }

    @Component
    private static class TestAuthenticationProvider extends DaoAuthenticationProvider {

        @PostConstruct
        void setUp() {
            setUserDetailsService(new InMemoryUserDetailsManager());
        }
    }

    private static File makeFile(final String name, final long lastModified) {
        final File file = mock(File.class);
        when(file.getName()).thenReturn(name);
        when(file.lastModified()).thenReturn(lastModified);
        when(file.exists()).thenReturn(true);
        return file;
    }

    @Bean
    @Primary
    public AsyncService mockAsyncService() {
        final AsyncService asyncService = mock(AsyncService.class);
        when(asyncService.scheduleAtFixedRate(any(), any(), any())).thenAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return mock(Future.class);
        });
        return asyncService;
    }

    @Bean
    @Primary
    public FileService mockFileService() throws IOException {
        final FileService fileService = mock(FileService.class);

        final File wldFile = makeFile("world.wld", 1);
        when(fileService.listFilesWithExtensions(WORLD_DIRECTORY.toFile(), "wld")).thenReturn(new File[]{wldFile});

        final File twldFile = makeFile("world.twld", 8);
        when(fileService.pathToFile(WORLD_DIRECTORY.resolve("world.wld"))).thenReturn(wldFile);
        when(fileService.pathToFile(WORLD_DIRECTORY.resolve("world.twld"))).thenReturn(twldFile);

        when(fileService.readFileInStream(wldFile)).thenReturn(new ByteArrayInputStream(new byte[0]));

        when(fileService.zip(wldFile, twldFile)).thenReturn(new byte[0]);
        return fileService;
    }

    @Bean
    @Primary
    public HttpService mockHttpService() {
        return mock(HttpService.class);
    }

    @Bean
    @Primary
    public TmuxService mockTmuxService() {
        return mock(TmuxService.class);
    }

    @Bean
    @Primary
    public SystemService mockSystemService() {
        final SystemService systemService = mock(SystemService.class);
        when(systemService.getOs()).thenReturn(OperatingSystem.LINUX);
        return systemService;
    }
}
