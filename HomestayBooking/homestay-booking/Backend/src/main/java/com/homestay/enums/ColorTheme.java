package com.homestay.enums;

/** FR-18 fixed palette for banner gradients (v1). */
public enum ColorTheme {
    red,
    blue,
    green,
    purple,
    orange;

    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            ColorTheme.valueOf(value.trim().toLowerCase());
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static String normalizeOrThrow(String value) {
        if (!isValid(value)) {
            throw new IllegalArgumentException(
                    "colorTheme phải là một trong: red, blue, green, purple, orange");
        }
        return value.trim().toLowerCase();
    }
}
