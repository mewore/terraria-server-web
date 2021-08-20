package io.github.mewore.tsw.services.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@Service
public class HttpService {

    private static final RequestSettings DEFAULT_REQUEST_SETTINGS = RequestSettings.builder().build();

    private static final RequestSettings HEAD_REQUEST_SETTINGS = RequestSettings.builder()
            .method(HttpMethod.HEAD)
            .build();

    private static final ObjectReader JSON_READER = new ObjectMapper().configure(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).reader();

    private final Logger logger = LogManager.getLogger(getClass());

    public void checkUrl(final URL url) throws IOException, HttpClientErrorException {
        try (final InputStream stream = requestAsStream(url, HEAD_REQUEST_SETTINGS)) {
            logger.info("The response header for URL {} is:\n{}", url,
                    new String(stream.readAllBytes(), StandardCharsets.US_ASCII));
        }
    }

    public InputStream requestAsStream(final URL url) throws IOException {
        return requestAsStream(url, DEFAULT_REQUEST_SETTINGS);
    }

    public InputStream requestAsStream(final URL url, final RequestSettings settings) throws IOException {
        if (!url.getProtocol().matches("^https?$")) {
            return url.openStream();
        }
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(settings.getMethod().name());
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
                return requestAsStream(new URL(newLocation), settings);
            }
        } else if (responseStatus.is4xxClientError()) {
            throw new HttpClientErrorException(responseStatus);
        } else if (responseStatus.is5xxServerError()) {
            throw new HttpServerErrorException(responseStatus);
        }
        return connection.getInputStream();
    }

    public <T> T get(final URL url, final TypeReference<T> typeReference) throws IOException {
        try (final InputStream stream = requestAsStream(url)) {
            return JSON_READER.createParser(stream).readValueAs(typeReference);
        }
    }
}
