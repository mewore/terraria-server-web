package io.github.mewore.tsw.javax.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.checkerframework.checker.nullness.qual.Nullable;

public class NullOrNotBlankValidator implements ConstraintValidator<NullOrNotBlank, String> {

    @Override
    public boolean isValid(final @Nullable String value, final ConstraintValidatorContext context) {
        return value == null || !value.isBlank();
    }
}
