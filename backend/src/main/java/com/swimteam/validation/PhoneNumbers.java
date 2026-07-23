package com.swimteam.validation;

import java.util.regex.Pattern;

/**
 * E.164 international phone numbers, e.g. +14155552671 or +447911123456.
 */
public final class PhoneNumbers {

    /**
     * Plus sign, non-zero country-code start, then 6–14 more digits (7–15 digits total).
     */
    public static final String E164_REGEX = "^\\+[1-9]\\d{6,14}$";

    public static final String E164_MESSAGE =
            "must be an international phone number in E.164 format (e.g. +14155552671)";

    private static final Pattern E164 = Pattern.compile(E164_REGEX);

    private PhoneNumbers() {}

    public static boolean isValidE164(String value) {
        return value != null && E164.matcher(value.trim()).matches();
    }
}
