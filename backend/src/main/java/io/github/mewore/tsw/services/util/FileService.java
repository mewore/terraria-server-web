package io.github.mewore.tsw.services.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

/**
 * A wrapper of Java NIO filesystem operations for easier mocking.
 */
@Service
public class FileService {

    public boolean fileExists(final Path filePath) {
        return Files.exists(filePath);
    }

    public String readFile(final Path filePath) throws IOException {
        return Files.lines(filePath).collect(Collectors.joining(System.lineSeparator()));
    }

    public void makeFile(final Path filePath, final String content) throws IOException {
        Files.writeString(filePath, content);
    }

}
