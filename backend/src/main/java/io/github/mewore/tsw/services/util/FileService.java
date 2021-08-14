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
import java.time.Instant;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * A wrapper of Java NIO filesystem operations for easier mocking.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Service
public class FileService {

    private static final int BUFFER_SIZE = 4096;

    private static final Path CACHE_ROOT = Path.of(System.getProperty("user.home"), ".tsw-cache");

    private final Logger logger = LogManager.getLogger(getClass());

    private final AsyncService asyncService;

    public boolean exists(final @NonNull File file) {
        return file.exists();
    }

    public boolean exists(final @NonNull Path filePath) {
        return Files.exists(filePath);
    }

    public boolean deleteRecursively(final Path target) {
        final File targetFile = target.toFile();
        if (!targetFile.exists()) {
            logger.warn(targetFile.getAbsolutePath() + " already does not exist. Skipping deleting it.");
            return true;
        }
        return deleteRecursively(target.toFile());
    }

    private boolean deleteRecursively(final File target) {
        boolean result = true;
        final File @Nullable [] allContents = target.listFiles();
        if (allContents != null) {
            for (final File file : allContents) {
                result = result && deleteRecursively(file);
            }
        }
        if (result && !target.delete()) {
            logger.error("Failed to delete '" + target + "'. Aborting recursive deletion.");
            result = false;
        }
        return result;
    }

    public String readFile(final @NonNull Path filePath) throws IOException {
        return Files.lines(filePath).collect(Collectors.joining(System.lineSeparator()));
    }

    public FileTail tail(final File file, final long startPosition, final FileTailEventConsumer eventConsumer) {
        final FileTail tail = new FileTail(file, startPosition, eventConsumer);
        asyncService.runInThread(tail);
        return tail;
    }

    public void makeFile(final @NonNull Path filePath, final @NonNull String content) throws IOException {
        Files.writeString(filePath, content);
    }

    public boolean makeFilesInDirectoryExecutable(final @NonNull File directory, final @NonNull Pattern fileNamePattern)
            throws IOException {

        final File @Nullable [] files = directory.listFiles((dir, name) -> fileNamePattern.matcher(name).matches());
        if (files == null || files.length == 0) {
            return false;
        }
        for (final File file : files) {
            if (!file.setExecutable(true)) {
                throw new IOException("Failed to make file executable: " + file.getAbsolutePath());
            }
        }
        return true;
    }

    public File reserveDirectory(final @NonNull Path directoryPath) throws IOException {
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
    public InputStream cache(final @NonNull Path cacheLocation,
            final @NonNull InputStreamSupplier streamSupplier,
            final @Nullable Long expectedSize) throws IOException {
        final File cacheFile = CACHE_ROOT.resolve(cacheLocation).toFile();
        if (cacheFile.exists() && (expectedSize == null || cacheFile.length() == expectedSize)) {
            logger.debug("Loading from cache: " + cacheFile.getAbsolutePath());
            return new FileInputStream(cacheFile);
        }
        logger.info("{} cache file: {}", cacheFile.exists() ? "Overwriting" : "Creating", cacheFile.getAbsolutePath());
        final @Nullable File parentFile = cacheFile.getParentFile();
        if (parentFile != null) {
            createDirectory(parentFile);
        }
        try (final InputStream stream = streamSupplier.supplyStream()) {
            flushStreamToFile(stream, cacheFile);
        }
        return new FileInputStream(cacheFile);
    }

    public boolean hasFileInCache(final Path cacheLocation) {
        return CACHE_ROOT.resolve(cacheLocation).toFile().isFile();
    }

    public void unzip(final @NonNull InputStream zipInputStream, final @NonNull File destination) throws IOException {
        unzip(zipInputStream, destination, null);
    }

    public void unzip(final @NonNull InputStream zipInputStream,
            final @NonNull File destination,
            final @Nullable String prefix) throws IOException {

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
                        final @Nullable File parentFile = target.getParentFile();
                        if (parentFile != null) {
                            createDirectory(parentFile);
                        }
                        flushStreamToFile(zipStream, target);
                    }
                }
                zipStream.closeEntry();
                entry = zipStream.getNextEntry();
            }
        }
    }

    public File pathToFile(final Path path) {
        return path.toFile();
    }

    public void setLastModified(final Path path, final Instant newLastModified) throws IOException {
        final File file = path.toFile();
        if (!file.setLastModified(newLastModified.toEpochMilli())) {
            throw new IOException("Failed to set the last modified time of " + file.getAbsolutePath());
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

    public File[] listFilesWithExtensions(final @NonNull File directory, final @NonNull String... extensions) {
        final File[] result = directory.listFiles(pathname -> pathname.isFile() &&
                Arrays.stream(extensions).anyMatch(extension -> pathname.getName().endsWith("." + extension)));
        return result == null ? new File[0] : result;
    }

    private void createDirectory(final @NonNull File directory) throws IOException {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Failed to create directory: " + directory.getAbsolutePath());
        }
    }

    private void writeFileToStream(final @NonNull File file, final @NonNull OutputStream target) throws IOException {
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
