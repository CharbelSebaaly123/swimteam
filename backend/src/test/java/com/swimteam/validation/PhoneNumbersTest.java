package com.swimteam.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PhoneNumbersTest {

    @Test
    void acceptsValidE164Numbers() {
        assertTrue(PhoneNumbers.isValidE164("+14155552671"));
        assertTrue(PhoneNumbers.isValidE164("+447911123456"));
        assertTrue(PhoneNumbers.isValidE164("+96171234567"));
    }

    @Test
    void rejectsLocalOrMalformedNumbers() {
        assertFalse(PhoneNumbers.isValidE164("555-0100"));
        assertFalse(PhoneNumbers.isValidE164("14155552671"));
        assertFalse(PhoneNumbers.isValidE164("+0123"));
        assertFalse(PhoneNumbers.isValidE164(""));
        assertFalse(PhoneNumbers.isValidE164(null));
        assertFalse(PhoneNumbers.isValidE164("+1"));
        assertFalse(PhoneNumbers.isValidE164("++14155552671"));
    }

    @Test
    void trimsSurroundingWhitespaceBeforeValidation() {
        assertTrue(PhoneNumbers.isValidE164(" +14155552671 "));
    }
}
