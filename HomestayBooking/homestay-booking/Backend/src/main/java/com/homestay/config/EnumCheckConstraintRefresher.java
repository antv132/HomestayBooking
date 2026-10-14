package com.homestay.config;

import com.homestay.entity.Notification;
import com.homestay.entity.Room;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Hibernate (6.2+) tạo CHECK constraint cho cột enum khi tạo bảng, nhưng
 * ddl-auto=update KHÔNG cập nhật constraint khi enum thêm giá trị mới.
 * Ghi giá trị enum mới (vd Room CLEANING_IN_PROGRESS) sẽ bị SQL Server chặn
 * → DataIntegrityViolationException → 409. Runner này drop + tạo lại các
 * CHECK constraint enum bị thiếu giá trị so với code hiện tại.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EnumCheckConstraintRefresher implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    private record EnumColumn(String table, String column, Enum<?>[] values) {}

    private static final List<EnumColumn> ENUM_COLUMNS = List.of(
            new EnumColumn("rooms", "status", Room.Status.values()),
            new EnumColumn("notifications", "type", Notification.Type.values())
    );

    @Override
    public void run(ApplicationArguments args) {
        for (EnumColumn ec : ENUM_COLUMNS) {
            try {
                refresh(ec);
            } catch (Exception ex) {
                // Không chặn app khởi động — chỉ cảnh báo để dev xử lý thủ công.
                log.warn("Không thể làm mới CHECK constraint {}.{}: {}",
                        ec.table(), ec.column(), ex.getMessage());
            }
        }
    }

    private void refresh(EnumColumn ec) {
        List<Map<String, Object>> constraints = jdbcTemplate.queryForList("""
                SELECT cc.name AS name, cc.definition AS definition
                FROM sys.check_constraints cc
                JOIN sys.columns c
                  ON c.object_id = cc.parent_object_id
                 AND c.column_id = cc.parent_column_id
                WHERE OBJECT_NAME(cc.parent_object_id) = ?
                  AND c.name = ?
                """, ec.table(), ec.column());

        if (constraints.isEmpty()) {
            return;
        }

        List<String> enumNames = Arrays.stream(ec.values()).map(Enum::name).toList();

        for (Map<String, Object> row : constraints) {
            String name = (String) row.get("name");
            String definition = String.valueOf(row.get("definition"));
            boolean missingValue = enumNames.stream()
                    .anyMatch(v -> !definition.contains("'" + v + "'"));
            if (!missingValue) {
                continue;
            }
            String inList = enumNames.stream()
                    .map(v -> "'" + v + "'")
                    .collect(Collectors.joining(","));
            jdbcTemplate.execute("ALTER TABLE " + ec.table() + " DROP CONSTRAINT [" + name + "]");
            jdbcTemplate.execute("ALTER TABLE " + ec.table()
                    + " ADD CONSTRAINT [" + name + "] CHECK (" + ec.column() + " IN (" + inList + "))");
            log.info("Đã làm mới CHECK constraint [{}] trên {}.{} với đủ giá trị enum hiện tại.",
                    name, ec.table(), ec.column());
        }
    }
}
