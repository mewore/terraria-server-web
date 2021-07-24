package io.github.mewore.tsw.jpa.converters;

import java.util.Collections;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.internal.stubbing.answers.ThrowsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class JsonConverterTest {

    @Test
    void testConvertToDatabaseColumn() {
        assertEquals("[\"a\"]",
                new JsonConverter<List<String>>().convertToDatabaseColumn(Collections.singletonList("a")));
    }

    @Test
    void testConvertToDatabaseColumn_null() {
        assertNull(new JsonConverter<List<String>>().convertToDatabaseColumn(null));
    }

    @Test
    void testConvertToDatabaseColumn_invalid() {
        final List<?> list = mock(List.class, new ThrowsException(new RuntimeException("oof")));
        final Exception exception = assertThrows(RuntimeException.class,
                () -> new JsonConverter<List<?>>().convertToDatabaseColumn(list));
        final @Nullable String exceptionMessage = exception.getMessage();
        assertNotNull(exceptionMessage);
        assertEquals("Failed to serialize an attribute of type org.mockito.codegen.List",
                exceptionMessage.substring(0, exceptionMessage.indexOf('$')));
    }

    @Test
    void testConvertToEntityAttribute() {
        assertEquals(Collections.singletonList("a"),
                new JsonConverter<List<String>>().convertToEntityAttribute("[\"a\"]"));
    }

    @Test
    void testConvertToEntityAttribute_null() {
        assertNull(new JsonConverter<List<String>>().convertToEntityAttribute(null));
    }

    @Test
    void testConvertToEntityAttribute_invalid() {
        final Exception exception = assertThrows(RuntimeException.class,
                () -> new JsonConverter<List<String>>().convertToEntityAttribute("a"));
        assertEquals("Failed to deserialize the string: a", exception.getMessage());
    }
}