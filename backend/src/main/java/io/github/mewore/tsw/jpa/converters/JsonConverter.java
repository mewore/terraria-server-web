package io.github.mewore.tsw.jpa.converters;

import javax.persistence.AttributeConverter;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import org.checkerframework.checker.nullness.qual.Nullable;

public class JsonConverter<T> implements AttributeConverter<@Nullable T, @Nullable String> {

    private static final ObjectWriter JSON_WRITER = new ObjectMapper().writer();

    private static final ObjectReader JSON_READER = new ObjectMapper().reader();

    @Override
    public @Nullable String convertToDatabaseColumn(final @Nullable T attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return JSON_WRITER.writeValueAsString(attribute);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(
                    "Failed to serialize an attribute of type " + attribute.getClass().getCanonicalName(), e);
        }
    }

    @Override
    public @Nullable T convertToEntityAttribute(final @Nullable String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return JSON_READER.readValue(JSON_READER.createParser(dbData), makeDeserializationTypeReference());
        } catch (final IOException e) {
            throw new RuntimeException("Failed to deserialize the string: " + dbData, e);
        }
    }

    protected TypeReference<T> makeDeserializationTypeReference() {
        return new TypeReference<>() {
        };
    }
}
