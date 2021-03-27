package io.github.mewore.tsw.services.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * A wrapper of Java NIO filesystem operations for easier mocking.
 */
@Service
public class FileService {

    private static final int BUFFER_SIZE = 4096;

    private static final Path CACHE_ROOT = Path.of(System.getProperty("user.home"), ".tsw-cache");

    private final Logger logger = LogManager.getLogger(getClass());

    public boolean fileExists(final Path filePath) {
        return Files.exists(filePath);
    }

    public String readFile(final Path filePath) throws IOException {
        return Files.lines(filePath).collect(Collectors.joining(System.lineSeparator()));
    }

    public void makeFile(final Path filePath, final String content) throws IOException {
        Files.writeString(filePath, content);
    }

    public File reserveDirectory(final Path directoryPath) throws IOException {
        final File directory = directoryPath.toFile();
        if (directory.exists()) {
            throw new IllegalStateException("The directory " + directory.getAbsolutePath() + " already exists");
        }
        if (!directory.mkdirs()) {
            throw new IOException("Failed to create directory " + directory.getAbsolutePath());
        }
        return directory;
    }

    /**
     * Gets a cached file or caches the specified stream and then returns a stream that reads the created cache file.
     *
     * @param cacheLocation  The location of the file in the cache.
     * @param streamSupplier The supplier of the data stream in case the file is not in the cache.
     * @return A stream of the data from the cached file.
     * @throws IOException If reading the file or creating/reading the input stream fails.
     */
    public InputStream cache(final Path cacheLocation,
            final InputStreamSupplier streamSupplier,
            @Nullable final Long expectedSize) throws IOException {
        final File cacheFile = CACHE_ROOT.resolve(cacheLocation).toFile();
        if (!cacheFile.exists() || (expectedSize != null && cacheFile.length() != expectedSize)) {
            logger.info("{} cache file: {}", cacheFile.exists() ? "Overwriting" : "Creating",
                    cacheFile.getAbsolutePath());
            createDirectory(cacheFile.getParentFile());
            try (final InputStream stream = streamSupplier.supplyStream()) {
                flushStreamToFile(stream, cacheFile);
            }
        }
        logger.debug("Loading from cache: " + cacheFile.getAbsolutePath());
        return new FileInputStream(cacheFile);
    }

    public void unzip(final InputStream zipInputStream, final File destination) throws IOException {
        unzip(zipInputStream, destination, null);
    }

    public void unzip(final InputStream zipInputStream, final File destination, @Nullable final String prefix)
            throws IOException {

        createDirectory(destination);
        try (final ZipInputStream zipStream = new ZipInputStream(zipInputStream)) {
            ZipEntry entry = zipStream.getNextEntry();
            while (entry != null) {
                if (prefix == null || entry.getName().startsWith(prefix)) {
                    String actualEntryPath = entry.getName().substring(prefix == null ? 0 : prefix.length());
                    if (actualEntryPath.startsWith("/")) {
                        actualEntryPath = actualEntryPath.substring(1);
                    }
                    final File target = destination.toPath().resolve(actualEntryPath).toFile();
                    if (entry.isDirectory()) {
                        createDirectory(target);
                    } else {
                        createDirectory(target.getParentFile());
                        flushStreamToFile(zipStream, target);
                    }
                }
                zipStream.closeEntry();
                entry = zipStream.getNextEntry();
            }
        }
    }

    public byte[] zip(final File... files) throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (final ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
            for (final File file : files) {
                zipOut.putNextEntry(new ZipEntry(file.getName()));
                writeFileToStream(file, zipOut);
            }
        }
        return outputStream.toByteArray();
    }

    public File[] listFiles(final File directory, final String... extensions) {
        final File[] result = directory.listFiles(pathname -> pathname.isFile() &&
                Arrays.stream(extensions).anyMatch(extension -> pathname.getName().endsWith("." + extension)));
        return result == null ? new File[0] : result;
    }

    private void createDirectory(final File directory) throws IOException {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Failed to create directory: " + directory.getAbsolutePath());
        }
    }

    private void writeFileToStream(final File file, final OutputStream target) throws IOException {
        try (final FileInputStream fis = new FileInputStream(file)) {
            final byte[] bytes = new byte[BUFFER_SIZE];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                target.write(bytes, 0, length);
            }
        }
    }

    private void flushStreamToFile(final InputStream stream, final File target) throws IOException {
        final BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(target));
        final byte[] streamBuffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = stream.read(streamBuffer)) != -1) {
            outputStream.write(streamBuffer, 0, read);
        }
        outputStream.close();
    }
}
