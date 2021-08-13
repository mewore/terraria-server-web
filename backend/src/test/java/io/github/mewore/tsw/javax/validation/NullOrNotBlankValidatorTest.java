package io.github.mewore.tsw.javax.validation;

import javax.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NullOrNotBlankValidatorTest {

    @Mock
    private ConstraintValidatorContext validatorContext;

    @Test
    void testIsValid() {
        assertTrue(new NullOrNotBlankValidator().isValid("Some value", validatorContext));
    }

    @Test
    void testIsValid_null() {
        assertTrue(new NullOrNotBlankValidator().isValid(null, validatorContext));
    }

    @Test
    void testIsValid_blank() {
        assertFalse(new NullOrNotBlankValidator().isValid("    ", validatorContext));
    }
}