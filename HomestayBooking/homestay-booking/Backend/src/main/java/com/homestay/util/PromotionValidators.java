package com.homestay.util;

import com.homestay.enums.ColorTheme;
import com.homestay.exception.BusinessException;

import java.util.Locale;

/** FR-18 Promotion field validation (ctaUrl + colorTheme). */
public final class PromotionValidators {

    private PromotionValidators() {}

    public static String normalizeColorTheme(String colorTheme) {
        try {
            return ColorTheme.normalizeOrThrow(colorTheme);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ex.getMessage());
        }
    }

    /**
     * Allow relative app paths ({@code /...}) or absolute {@code http://}/{@code https://} URLs.
     * Reject {@code javascript:}, {@code data:}, and other schemes.
     */
    public static String normalizeCtaUrl(String ctaUrl) {
        if (ctaUrl == null || ctaUrl.isBlank()) {
            throw new BusinessException("ctaUrl is required");
        }
        String trimmed = ctaUrl.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("vbscript:")) {
            throw new BusinessException("ctaUrl không được dùng scheme không an toàn");
        }
        if (trimmed.startsWith("/")) {
            return trimmed;
        }
        if (lower.startsWith("https://") || lower.startsWith("http://")) {
            return trimmed;
        }
        throw new BusinessException("ctaUrl phải là đường dẫn bắt đầu bằng / hoặc URL http(s)://");
    }
}
