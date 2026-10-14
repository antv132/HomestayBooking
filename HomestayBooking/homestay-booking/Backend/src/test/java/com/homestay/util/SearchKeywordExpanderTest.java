package com.homestay.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SearchKeywordExpanderTest {

    @Test
    void expand_withNullKeyword_returnsEmptyList() {
        List<String> result = SearchKeywordExpander.expand(null);

        assertThat(result).isEmpty();
    }

    @Test
    void expand_withBlankKeyword_returnsEmptyList() {
        List<String> result = SearchKeywordExpander.expand("   ");

        assertThat(result).isEmpty();
    }

    @Test
    void expand_withSimpleKeyword_returnsOriginalAndAscii() {
        List<String> result = SearchKeywordExpander.expand("Hà Nội");

        assertThat(result).contains("Hà Nội", "Ha Noi");
    }

    @Test
    void expand_withCityAlias_returnsAliases() {
        List<String> result = SearchKeywordExpander.expand("Ha Noi");

        assertThat(result).contains("Ha Noi");
        assertThat(result).contains("Old Quarter");
        assertThat(result).contains("Hoàn Kiếm");
    }

    @Test
    void expand_withDaNangKeyword_returnsAliases() {
        List<String> result = SearchKeywordExpander.expand("Da Nang");

        assertThat(result).contains("Da Nang");
        assertThat(result).contains("Sunset Resort");
    }

    @Test
    void expand_withHoiAnKeyword_returnsAliases() {
        List<String> result = SearchKeywordExpander.expand("Hoi An");

        assertThat(result).contains("Hoi An");
        assertThat(result).contains("Garden Villa");
    }

    @Test
    void expand_withPhuQuocKeyword_returnsAliases() {
        List<String> result = SearchKeywordExpander.expand("Phu Quoc");

        assertThat(result).contains("Phu Quoc");
        assertThat(result).contains("Beach House");
    }

    @Test
    void expand_withDaLatKeyword_returnsAliases() {
        List<String> result = SearchKeywordExpander.expand("Da Lat");

        assertThat(result).contains("Da Lat");
        assertThat(result).contains("Mountain View");
    }

    @Test
    void expand_withNhaTrangKeyword_returnsAliases() {
        List<String> result = SearchKeywordExpander.expand("Nha Trang");

        assertThat(result).contains("Nha Trang");
    }

    @Test
    void expand_withNonCityKeyword_returnsOriginalAndAscii() {
        List<String> result = SearchKeywordExpander.expand("Phòng đẹp");

        assertThat(result).contains("Phòng đẹp");
        assertThat(result).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void expand_withAsciiKeyword_returnsSingleItem() {
        List<String> result = SearchKeywordExpander.expand("room");

        assertThat(result).containsExactly("room");
    }

    @Test
    void expand_trimsWhitespace() {
        List<String> result = SearchKeywordExpander.expand("  Hà Nội  ");

        assertThat(result).contains("Hà Nội");
    }

    @Test
    void expand_isCaseInsensitiveForCityAliases() {
        List<String> result = SearchKeywordExpander.expand("HANOI");

        assertThat(result).contains("HANOI", "Old Quarter");
    }

    @Test
    void removeAccents_withAccentedString_removesAccents() {
        String result = SearchKeywordExpander.removeAccents("Hà Nội");

        assertThat(result).isEqualTo("Ha Noi");
    }

    @Test
    void removeAccents_withNullString_returnsEmpty() {
        String result = SearchKeywordExpander.removeAccents(null);

        assertThat(result).isEmpty();
    }

    @Test
    void removeAccents_withAsciiString_returnsSame() {
        String result = SearchKeywordExpander.removeAccents("Hello World");

        assertThat(result).isEqualTo("Hello World");
    }

    @Test
    void removeAccents_withVietnameseChars_removesAllDiacritics() {
        String result = SearchKeywordExpander.removeAccents("Hà Nội");

        assertThat(result).isEqualTo("Ha Noi");
    }
}
