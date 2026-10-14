package com.homestay.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "system_settings",
    indexes = { @Index(name = "idx_settings_key", columnList = "setting_key", unique = true) }
)
@Getter
@Setter
@NoArgsConstructor
public class SystemSetting {

    /*
     * Các key được dùng trong hệ thống:
     * DEPOSIT_PERCENTAGE   -> "40" (phần trăm đặt cọc)
     * SYSTEM_NAME          -> tên hệ thống
     * SUPPORT_EMAIL        -> email hỗ trợ
     * BANK_ACCOUNT_NUMBER  -> số tài khoản ngân hàng
     * BANK_ACCOUNT_NAME    -> tên chủ tài khoản
     * BANK_NAME            -> tên ngân hàng
     */

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String key;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String value;

    @Column(name = "description", length = 500)
    private String description;

    // Manager nào cập nhật lần cuối
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
