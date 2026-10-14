package com.homestay.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Mở rộng từ khóa tìm kiếm — alias thành phố, bỏ dấu tiếng Việt. */
public final class SearchKeywordExpander {

    private static final Map<String, List<String>> CITY_ALIASES = Map.ofEntries(
            Map.entry("ha noi", List.of("Old Quarter", "Hàng Bạc", "Hoàn Kiếm")),
            Map.entry("hanoi", List.of("Old Quarter", "Hàng Bạc", "Hoàn Kiếm")),
            Map.entry("da nang", List.of("Sunset Resort", "Nguyễn Tất Thành")),
            Map.entry("hoi an", List.of("Garden Villa", "Phan Bội Châu")),
            Map.entry("phu quoc", List.of("Beach House", "Trần Hưng Đạo")),
            Map.entry("da lat", List.of("Mountain View", "Trần Phú")),
            Map.entry("nha trang", List.of("Nha Trang"))
    );

    private SearchKeywordExpander() {}

    public static List<String> expand(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        String trimmed = keyword.trim();
        terms.add(trimmed);

        String ascii = removeAccents(trimmed);
        if (!ascii.equals(trimmed)) {
            terms.add(ascii);
        }

        String norm = ascii.toLowerCase(Locale.ROOT);
        List<String> cityTerms = CITY_ALIASES.get(norm);
        if (cityTerms != null) {
            terms.addAll(cityTerms);
        }

        return new ArrayList<>(terms);
    }

    public static String removeAccents(String input) {
        if (input == null) return "";
        String nfd = Normalizer.normalize(input, Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{M}", "");
    }
}
