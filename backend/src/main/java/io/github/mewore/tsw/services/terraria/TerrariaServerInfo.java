package io.github.mewore.tsw.services.terraria;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.mewore.tsw.exceptions.InvalidInstanceException;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
class TerrariaServerInfo {

    private static final Pattern TERRARIA_SERVER_URL_REGEX =
            Pattern.compile("^https?://(www\\.)?terraria\\.org/[^?]+/(terraria-server-(\\d+).zip)(\\?\\d+)?$");

    private final URL url;

    private final String zipName;

    private final String rawVersion;

    private final String formattedVersion;

    static TerrariaServerInfo fromInstance(final TerrariaInstanceEntity instance) throws InvalidInstanceException {
        final String serverUrlString = instance.getTerrariaServerUrl();
        final Matcher serverZipUrlMatcher = TERRARIA_SERVER_URL_REGEX.matcher(serverUrlString);
        if (!serverZipUrlMatcher.find()) {
            throw new InvalidInstanceException(
                    "The URL " + serverUrlString + " does not match the regular expression " +
                            TERRARIA_SERVER_URL_REGEX);
        }

        final String serverZipName = serverZipUrlMatcher.group(2);
        final String serverRawVersion = serverZipUrlMatcher.group(3);

        if (serverZipName == null || serverRawVersion == null) {
            throw new RuntimeException("A server URL matcher group is null");
        }

        final String serverFormattedVersion = String.join(".", serverRawVersion.split(""));

        final URL serverUrl;
        try {
            serverUrl = new URL(serverUrlString);
        } catch (final MalformedURLException e) {
            throw new InvalidInstanceException(serverUrlString + " is not a valid URL", e);
        }

        return new TerrariaServerInfo(serverUrl, serverZipName, serverRawVersion, serverFormattedVersion);
    }
}
