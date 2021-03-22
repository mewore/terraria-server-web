package io.github.mewore.tsw.services.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class HttpService {

    private static final ObjectReader JSON_READER =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).reader();

    private final Logger logger = LogManager.getLogger(getClass());

    public byte[] requestRaw(final URL url) throws IOException {
        try (final InputStream stream = url.openStream()) {
            return stream.readAllBytes();
        }
    }

    public InputStream requestAsStream(final URL url) throws IOException {
        if (!url.getProtocol().matches("^https?$")) {
            return url.openStream();
        }
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();

        final int responseCode = connection.getResponseCode();
        final HttpStatus responseStatus = HttpStatus.resolve(responseCode);
        if (responseStatus == null) {
            logger.warn("Could not resolve the HTTP response code [{}] of URL {}", responseCode, url);
        } else if (responseStatus.is3xxRedirection()) {
            final String newLocation = connection.getHeaderField("Location");
            if (newLocation == null) {
                logger.warn("Received a redirection HTTP code [{}] without a 'Location' header when requesting: {}",
                        responseCode, url);
            } else {
                logger.info("Redirected from {} to {}", url, newLocation);
                return requestAsStream(new URL(newLocation));
            }
        }
        return connection.getInputStream();
    }

    public <T> T get(final URL url, final TypeReference<T> typeReference) throws IOException {
        final byte[] raw = requestRaw(url);
        return JSON_READER.createParser(raw).readValueAs(typeReference);
    }
}
