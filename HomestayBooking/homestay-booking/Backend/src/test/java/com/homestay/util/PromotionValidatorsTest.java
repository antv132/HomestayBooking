package com.homestay.util;

import com.homestay.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromotionValidatorsTest {

    @Test
    void normalizeCtaUrl_withRelativePath_returnsPath() {
        String result = PromotionValidators.normalizeCtaUrl("/rooms/123");

        assertThat(result).isEqualTo("/rooms/123");
    }

    @Test
    void normalizeCtaUrl_withHttpsUrl_returnsUrl() {
        String result = PromotionValidators.normalizeCtaUrl("https://example.com/promo");

        assertThat(result).isEqualTo("https://example.com/promo");
    }

    @Test
    void normalizeCtaUrl_withHttpUrl_returnsUrl() {
        String result = PromotionValidators.normalizeCtaUrl("http://example.com/promo");

        assertThat(result).isEqualTo("http://example.com/promo");
    }

    @Test
    void normalizeCtaUrl_withJavascriptScheme_throwsException() {
        assertThatThrownBy(() -> PromotionValidators.normalizeCtaUrl("javascript:alert('xss')"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không an toàn");
    }

    @Test
    void normalizeCtaUrl_withDataScheme_throwsException() {
        assertThatThrownBy(() -> PromotionValidators.normalizeCtaUrl("data:text/html,<script>alert('xss')</script>"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không an toàn");
    }

    @Test
    void normalizeCtaUrl_withVbscriptScheme_throwsException() {
        assertThatThrownBy(() -> PromotionValidators.normalizeCtaUrl("vbscript:msgbox"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không an toàn");
    }

    @Test
    void normalizeCtaUrl_withMixedCaseJavascript_throwsException() {
        assertThatThrownBy(() -> PromotionValidators.normalizeCtaUrl("JaVaScRiPt:alert(1)"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không an toàn");
    }

    @Test
    void normalizeCtaUrl_withNullUrl_throwsException() {
        assertThatThrownBy(() -> PromotionValidators.normalizeCtaUrl(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("required");
    }

    @Test
    void normalizeCtaUrl_withBlankUrl_throwsException() {
        assertThatThrownBy(() -> PromotionValidators.normalizeCtaUrl("   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("required");
    }

    @Test
    void normalizeCtaUrl_withInvalidScheme_throwsException() {
        assertThatThrownBy(() -> PromotionValidators.normalizeCtaUrl("ftp://example.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("http(s)://");
    }

    @Test
    void normalizeCtaUrl_trimsWhitespace() {
        String result = PromotionValidators.normalizeCtaUrl("  /rooms/123  ");

        assertThat(result).isEqualTo("/rooms/123");
    }

    @Test
    void normalizeColorTheme_withValidTheme_returnsNormalized() {
        String result = PromotionValidators.normalizeColorTheme("RED");

        assertThat(result).isNotNull();
    }

    @Test
    void normalizeColorTheme_withInvalidTheme_throwsException() {
        assertThatThrownBy(() -> PromotionValidators.normalizeColorTheme("INVALID_COLOR"))
                .isInstanceOf(BusinessException.class);
    }
}
