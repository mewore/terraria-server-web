package io.github.mewore.tsw.services.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.springframework.stereotype.Service;

@Service
public class HttpService {

    public byte[] requestRaw(final URL url) throws IOException {
        try (final InputStream stream = url.openStream()) {
            return stream.readAllBytes();
        }
    }
}
