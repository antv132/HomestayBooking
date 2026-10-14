package com.homestay.config;

import com.homestay.entity.ChecklistItemDefinition;
import com.homestay.repository.ChecklistItemDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Seed danh mục checklist kiểm tra phòng (global) nếu chưa có. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChecklistItemSeedRunner implements ApplicationRunner {

    private final ChecklistItemDefinitionRepository repository;

    private record Seed(String code, String label, String icon, int sort) {}

    private static final Seed[] DEFAULTS = {
            new Seed("tv", "Tivi & Điều khiển", "📺", 1),
            new Seed("ac", "Điều hòa & Remote", "❄️", 2),
            new Seed("minibar", "Tủ lạnh & Mini bar", "🍹", 3),
            new Seed("bathroom", "Thiết bị vệ sinh", "🚿", 4),
            new Seed("beds", "Giường & Chăn ga gối", "🛏️", 5),
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.countGlobal() > 0) {
            return;
        }
        for (Seed seed : DEFAULTS) {
            ChecklistItemDefinition item = new ChecklistItemDefinition();
            item.setCode(seed.code());
            item.setLabel(seed.label());
            item.setIcon(seed.icon());
            item.setSortOrder(seed.sort());
            item.setIsActive(true);
            repository.save(item);
        }
        log.info("Seeded {} global inspection checklist items", DEFAULTS.length);
    }
}
