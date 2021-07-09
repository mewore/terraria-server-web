package io.github.mewore.tsw.services.terraria;

import org.junit.jupiter.api.Test;

import io.github.mewore.tsw.exceptions.InvalidInstanceException;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TerrariaServerInfoTest {

    private static TerrariaInstanceEntity makeInstanceWithUrl(final String url) {
        final TerrariaInstanceEntity instance = mock(TerrariaInstanceEntity.class);
        when(instance.getTerrariaServerUrl()).thenReturn(url);
        return instance;
    }

    @Test
    void testFromInstance() throws InvalidInstanceException {
        final TerrariaServerInfo serverInfo =
                TerrariaServerInfo.fromInstance(makeInstanceWithUrl("http://terraria.org/a/terraria-server-123.zip?0"));
        assertEquals("http://terraria.org/a/terraria-server-123.zip?0", serverInfo.getUrl().toString());
        assertEquals("terraria-server-123.zip", serverInfo.getZipName());
        assertEquals("123", serverInfo.getRawVersion());
        assertEquals("1.2.3", serverInfo.getFormattedVersion());
    }

    @Test
    void testFromInstance_urlNotMatchingRegExp() {
        final Exception exception = assertThrows(InvalidInstanceException.class,
                () -> TerrariaServerInfo.fromInstance(makeInstanceWithUrl("(a)")));
        assertEquals("The URL (a) does not match the regular expression " +
                        "^https?://(www\\.)?terraria\\.org/[^?]+/(terraria-server-(\\d+).zip)(\\?\\d+)?$",
                exception.getMessage());
    }
}