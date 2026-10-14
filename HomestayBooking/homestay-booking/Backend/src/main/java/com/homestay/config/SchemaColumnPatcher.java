package com.homestay.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Hibernate {@code ddl-auto=update} sometimes fails to add new columns on SQL Server
 * (existing tables). Auth OTP rate-limit fields must exist or every login/join on users fails.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class SchemaColumnPatcher implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        addColumnIfMissing(
                "users",
                "otp_send_count",
                "INT NOT NULL CONSTRAINT DF_users_otp_send_count DEFAULT 0");
        addColumnIfMissing(
                "users",
                "otp_send_window_start",
                "DATETIME2 NULL");
        // FR-02: full_name max 255 (contract/data-model); ddl-auto may not widen existing columns
        widenNvarcharIfNarrower("users", "full_name", 255);
        // 023 US6 dispute fields (entity present; ddl-auto may not have added them)
        addColumnIfMissing(
                "damage_reports",
                "dispute_note",
                "NVARCHAR(MAX) NULL");
        addColumnIfMissing(
                "damage_reports",
                "disputed_at",
                "DATETIME2 NULL");
        // FR-07: unique floor number per property (ddl-auto may not create constraint)
        addUniqueIndexIfMissing(
                "floors",
                "uk_floors_property_floor_number",
                "property_id, floor_number");
        // FR-08: soft-delete + unique room number among active rooms only
        addColumnIfMissing("rooms", "deleted_at", "DATETIME2 NULL");
        // FR-11: payments.invoice_id (ddl-auto may miss FK column)
        addColumnIfMissing("payments", "invoice_id", "UNIQUEIDENTIFIER NULL");
        addUniqueIndexIfMissing(
                "invoices",
                "uk_invoices_booking_type",
                "booking_id, type");
        // FR-13: employee work note + soft-delete
        addColumnIfMissing("maintenance_tickets", "work_note", "NVARCHAR(MAX) NULL");
        addColumnIfMissing("maintenance_tickets", "deleted_at", "DATETIME2 NULL");
        // FR-14: review moderation audit + room rating aggregates
        addColumnIfMissing("reviews", "moderated_by", "UNIQUEIDENTIFIER NULL");
        addColumnIfMissing("reviews", "moderated_at", "DATETIME2 NULL");
        addColumnIfMissing("rooms", "average_rating", "DECIMAL(3,2) NULL");
        addColumnIfMissing(
                "rooms",
                "total_reviews",
                "INT NOT NULL CONSTRAINT DF_rooms_total_reviews DEFAULT 0");
        ensureActiveRoomNumberUniqueIndex();
        // FR-15: notification idempotency + wider type
        addColumnIfMissing("notifications", "dedupe_key", "NVARCHAR(128) NULL");
        widenNvarcharIfNarrower("notifications", "type", 64);
        addFilteredUniqueIndexIfMissing(
                "notifications",
                "uq_notifications_dedupe_key",
                "dedupe_key",
                "dedupe_key IS NOT NULL");
        // FR-20: at most one ACTIVE employee_property_assignment per employee
        addFilteredUniqueIndexIfMissing(
                "employee_property_assignments",
                "uq_epa_one_active_per_employee",
                "employee_id",
                "status = 'ACTIVE'");
        // FR-17: activity_logs.ip_address for admin audit
        addColumnIfMissing("activity_logs", "ip_address", "NVARCHAR(45) NULL");
        // Edu slim-down: PricingRule deferred (use Room.pricePerNight); PaymentReceipt deferred TD-011
        dropTableIfExists("payment_receipts");
        dropTableIfExists("pricing_rules");
        // columnDefinition="TEXT" and JSON columns are non-Unicode on SQL Server -> Vietnamese becomes '?'
        convertNonUnicodeLobColumnsToNvarchar();
    }

    /** TEXT / VARCHAR(MAX) cannot hold Vietnamese under the default collation; LOB columns have no indexes, so ALTER is safe. */
    private void convertNonUnicodeLobColumnsToNvarchar() {
        try {
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                    """
                    SELECT TABLE_NAME, COLUMN_NAME, IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = 'dbo'
                      AND (DATA_TYPE = 'text' OR (DATA_TYPE = 'varchar' AND CHARACTER_MAXIMUM_LENGTH = -1))
                    """);
            for (Map<String, Object> col : columns) {
                String table = (String) col.get("TABLE_NAME");
                String column = (String) col.get("COLUMN_NAME");
                String nullability = "YES".equals(col.get("IS_NULLABLE")) ? "NULL" : "NOT NULL";
                try {
                    jdbcTemplate.execute("ALTER TABLE " + table + " ALTER COLUMN " + column
                            + " NVARCHAR(MAX) " + nullability);
                    log.info("Converted column {}.{} to NVARCHAR(MAX)", table, column);
                } catch (Exception ex) {
                    log.warn("Could not convert column {}.{} to NVARCHAR(MAX): {}", table, column, ex.getMessage());
                }
            }
        } catch (Exception ex) {
            log.warn("Could not scan for non-Unicode LOB columns: {}", ex.getMessage());
        }
    }

    /** Hibernate ddl-auto=update never drops tables when entities are removed. */
    private void dropTableIfExists(String table) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
                    WHERE TABLE_SCHEMA = 'dbo' AND TABLE_NAME = ?
                    """,
                    Integer.class,
                    table);
            if (count == null || count == 0) {
                return;
            }
            jdbcTemplate.execute("DROP TABLE dbo." + table);
            log.info("Dropped unused table {}", table);
        } catch (Exception ex) {
            log.warn("Could not drop table {}: {}", table, ex.getMessage());
        }
    }

    /**
     * Drop legacy UNIQUE (property_id, room_number) and replace with filtered unique
     * so soft-deleted room numbers can be reused.
     */
    private void ensureActiveRoomNumberUniqueIndex() {
        try {
            dropIndexIfExists("rooms", "uq_room_number_property");
            dropIndexIfExists("rooms", "UK_rooms_room_number_property_id");
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*) FROM sys.indexes
                    WHERE name = ? AND object_id = OBJECT_ID(?)
                    """,
                    Integer.class,
                    "uq_rooms_property_room_number_active",
                    "dbo.rooms");
            if (count != null && count > 0) {
                return;
            }
            jdbcTemplate.execute(
                    """
                    CREATE UNIQUE INDEX uq_rooms_property_room_number_active
                    ON rooms (property_id, room_number)
                    WHERE deleted_at IS NULL
                    """);
            log.info("Added filtered unique index uq_rooms_property_room_number_active on rooms");
        } catch (Exception ex) {
            log.warn("Could not ensure active room-number unique index: {}", ex.getMessage());
        }
    }

    private void dropIndexIfExists(String table, String indexName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*) FROM sys.indexes
                    WHERE name = ? AND object_id = OBJECT_ID(?)
                    """,
                    Integer.class,
                    indexName,
                    "dbo." + table);
            if (count != null && count > 0) {
                jdbcTemplate.execute("DROP INDEX " + indexName + " ON " + table);
                log.info("Dropped index {} on {}", indexName, table);
                return;
            }
            Integer constraintCount = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*) FROM sys.key_constraints
                    WHERE name = ? AND parent_object_id = OBJECT_ID(?)
                    """,
                    Integer.class,
                    indexName,
                    "dbo." + table);
            if (constraintCount != null && constraintCount > 0) {
                jdbcTemplate.execute("ALTER TABLE " + table + " DROP CONSTRAINT " + indexName);
                log.info("Dropped constraint {} on {}", indexName, table);
            }
        } catch (Exception ex) {
            log.warn("Could not drop index/constraint {} on {}: {}", indexName, table, ex.getMessage());
        }
    }

    private void addUniqueIndexIfMissing(String table, String indexName, String columns) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*) FROM sys.indexes
                    WHERE name = ? AND object_id = OBJECT_ID(?)
                    """,
                    Integer.class,
                    indexName,
                    "dbo." + table);
            if (count != null && count > 0) {
                return;
            }
            jdbcTemplate.execute(
                    "CREATE UNIQUE INDEX " + indexName + " ON " + table + " (" + columns + ")");
            log.info("Added unique index {} on {}", indexName, table);
        } catch (Exception ex) {
            log.warn("Could not ensure unique index {} on {}: {}", indexName, table, ex.getMessage());
        }
    }

    private void addFilteredUniqueIndexIfMissing(
            String table, String indexName, String columns, String whereClause) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*) FROM sys.indexes
                    WHERE name = ? AND object_id = OBJECT_ID(?)
                    """,
                    Integer.class,
                    indexName,
                    "dbo." + table);
            if (count != null && count > 0) {
                return;
            }
            jdbcTemplate.execute(
                    "CREATE UNIQUE INDEX " + indexName + " ON " + table
                            + " (" + columns + ") WHERE " + whereClause);
            log.info("Added filtered unique index {} on {}", indexName, table);
        } catch (Exception ex) {
            log.warn("Could not ensure filtered unique index {} on {}: {}",
                    indexName, table, ex.getMessage());
        }
    }

    private void addColumnIfMissing(String table, String column, String sqlTypeAndDefault) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_NAME = ? AND COLUMN_NAME = ?
                    """,
                    Integer.class,
                    table,
                    column);
            if (count != null && count > 0) {
                return;
            }
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD " + column + " " + sqlTypeAndDefault);
            log.info("Added missing column {}.{}", table, column);
        } catch (Exception ex) {
            log.warn("Could not ensure column {}.{}: {}", table, column, ex.getMessage());
        }
    }

    private void widenNvarcharIfNarrower(String table, String column, int minLength) {
        try {
            Integer current = jdbcTemplate.queryForObject(
                    """
                    SELECT CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_NAME = ? AND COLUMN_NAME = ?
                    """,
                    Integer.class,
                    table,
                    column);
            if (current == null || current < 0 || current >= minLength) {
                return;
            }
            jdbcTemplate.execute(
                    "ALTER TABLE " + table + " ALTER COLUMN " + column
                            + " NVARCHAR(" + minLength + ") NOT NULL");
            log.info("Widened column {}.{} to NVARCHAR({})", table, column, minLength);
        } catch (Exception ex) {
            log.warn("Could not widen column {}.{}: {}", table, column, ex.getMessage());
        }
    }
}
