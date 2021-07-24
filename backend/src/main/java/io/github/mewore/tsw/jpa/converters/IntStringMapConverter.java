package io.github.mewore.tsw.jpa.converters;

import javax.persistence.Converter;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import org.checkerframework.checker.nullness.qual.Nullable;

@Converter(autoApply = true)
public class IntStringMapConverter extends JsonConverter<Map<Integer, String>> {

    @Override
    public @Nullable Map<Integer, String> convertToEntityAttribute(final @Nullable String dbData) {
        final @Nullable Map<Integer, String> result = super.convertToEntityAttribute(dbData);
        return result == null ? null : Collections.unmodifiableMap(result);
    }

    @Override
    protected TypeReference<Map<Integer, String>> makeDeserializationTypeReference() {
        return new TypeReference<>() {
        };
    }
}
