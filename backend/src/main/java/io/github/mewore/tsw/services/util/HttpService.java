package io.github.mewore.tsw.services.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import org.springframework.stereotype.Service;

@Service
public class HttpService {

    private static final ObjectReader JSON_READER =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).reader();

    public byte[] requestRaw(final URL url) throws IOException {
        try (final InputStream stream = url.openStream()) {
            return stream.readAllBytes();
        }
    }

    public <T> T get(final URL url, final TypeReference<T> typeReference) throws IOException {
        final byte[] raw = requestRaw(url);
        return JSON_READER.createParser(raw).readValueAs(typeReference);
    }
}
