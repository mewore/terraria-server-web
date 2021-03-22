package io.github.mewore.tsw.jpa.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.nio.file.Path;

@Converter(autoApply = true)
public class PathConverter implements AttributeConverter<Path, String> {

    @Override
    public String convertToDatabaseColumn(final Path attribute) {
        return attribute.toAbsolutePath().toString();
    }

    @Override
    public Path convertToEntityAttribute(final String dbData) {
        return Path.of(dbData);
    }
}
