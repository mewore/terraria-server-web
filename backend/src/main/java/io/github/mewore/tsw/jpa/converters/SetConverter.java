package io.github.mewore.tsw.jpa.converters;

import javax.persistence.Converter;
import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;

import org.checkerframework.checker.nullness.qual.Nullable;

@Converter(autoApply = true)
public class SetConverter<T> extends JsonConverter<Set<T>> {

    @Override
    public @Nullable Set<T> convertToEntityAttribute(final @Nullable String dbData) {
        final @Nullable Set<T> result = super.convertToEntityAttribute(dbData);
        return result == null ? null : Collections.unmodifiableSet(result);
    }

    @Override
    protected TypeReference<Set<T>> makeDeserializationTypeReference() {
        return new TypeReference<>() {
        };
    }
}
