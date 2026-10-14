package com.homestay.service;

import com.homestay.dto.request.UpdateSystemSettingsRequest;
import com.homestay.dto.response.SystemSettingsResponse;
import com.homestay.entity.SystemSetting;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminSettingsService {

    public static final String KEY_DEPOSIT = "DEPOSIT_PERCENTAGE";
    public static final String KEY_CANCEL = "CANCEL_TIMEOUT_HOURS";
    public static final String KEY_SYSTEM_NAME = "SYSTEM_NAME";
    public static final String KEY_SUPPORT_EMAIL = "SUPPORT_EMAIL";
    public static final String KEY_BANK_ACCOUNT_NUMBER = "BANK_ACCOUNT_NUMBER";
    public static final String KEY_BANK_ACCOUNT_NAME = "BANK_ACCOUNT_NAME";
    public static final String KEY_BANK_NAME = "BANK_NAME";

    private static final List<String> ALL_KEYS = List.of(
            KEY_DEPOSIT, KEY_CANCEL, KEY_SYSTEM_NAME, KEY_SUPPORT_EMAIL,
            KEY_BANK_ACCOUNT_NUMBER, KEY_BANK_ACCOUNT_NAME, KEY_BANK_NAME);

    private static final int DEFAULT_DEPOSIT = 40;
    private static final int DEFAULT_CANCEL = 24;

    private final SystemSettingRepository systemSettingRepository;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public SystemSettingsResponse getSettings() {
        Map<String, String> map = loadKeyMap();
        LocalDateTime lastUpdated = systemSettingRepository.findByKeyIn(ALL_KEYS).stream()
                .map(SystemSetting::getUpdatedAt)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        return SystemSettingsResponse.builder()
                .depositPercentage(parseIntOrDefault(map.get(KEY_DEPOSIT), DEFAULT_DEPOSIT))
                .cancelTimeoutHours(parseIntOrDefault(map.get(KEY_CANCEL), DEFAULT_CANCEL))
                .systemName(map.getOrDefault(KEY_SYSTEM_NAME, "Homestay Booking System"))
                .supportEmail(map.getOrDefault(KEY_SUPPORT_EMAIL, ""))
                .bankAccountNumber(map.getOrDefault(KEY_BANK_ACCOUNT_NUMBER, ""))
                .bankAccountName(map.getOrDefault(KEY_BANK_ACCOUNT_NAME, ""))
                .bankName(map.getOrDefault(KEY_BANK_NAME, ""))
                .updatedAt(lastUpdated)
                .build();
    }

    /**
     * Public accessor for deposit percentage as BigDecimal fraction (e.g. 0.40).
     */
    @Transactional(readOnly = true)
    public BigDecimal getDepositPercentage() {
        String raw = systemSettingRepository.findByKey(KEY_DEPOSIT)
                .map(SystemSetting::getValue)
                .orElse(null);
        int pct = parseIntOrDefault(raw, DEFAULT_DEPOSIT);
        return BigDecimal.valueOf(pct).divide(BigDecimal.valueOf(100));
    }

    @Transactional
    public SystemSettingsResponse updateSettings(UpdateSystemSettingsRequest req, User admin) {
        if (req.getDepositPercentage() != null) {
            int v = req.getDepositPercentage();
            if (v < 10 || v > 50) {
                throw new BusinessException("depositPercentage phải từ 10 đến 50");
            }
            upsert(KEY_DEPOSIT, String.valueOf(v), "Deposit percentage for new bookings", admin);
        }
        if (req.getCancelTimeoutHours() != null) {
            int v = req.getCancelTimeoutHours();
            if (v < 1 || v > 168) {
                throw new BusinessException("cancelTimeoutHours phải từ 1 đến 168");
            }
            upsert(KEY_CANCEL, String.valueOf(v), "Hours allowed to cancel after booking created", admin);
        }
        if (req.getSystemName() != null && !req.getSystemName().isBlank()) {
            upsert(KEY_SYSTEM_NAME, req.getSystemName().trim(), "System display name", admin);
        }
        if (req.getSupportEmail() != null) {
            String email = req.getSupportEmail().trim();
            if (!email.isEmpty() && !email.matches("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
                throw new BusinessException("Email hỗ trợ không hợp lệ");
            }
            upsert(KEY_SUPPORT_EMAIL, email, "Support contact email", admin);
        }
        if (req.getBankAccountNumber() != null) {
            upsert(KEY_BANK_ACCOUNT_NUMBER, req.getBankAccountNumber().trim(), "Bank account number", admin);
        }
        if (req.getBankAccountName() != null) {
            upsert(KEY_BANK_ACCOUNT_NAME, req.getBankAccountName().trim(), "Bank account holder name", admin);
        }
        if (req.getBankName() != null) {
            upsert(KEY_BANK_NAME, req.getBankName().trim(), "Bank name", admin);
        }

        activityLogService.log(admin, ActivityLogService.SETTINGS_UPDATED,
                "SystemSetting", null, "Admin updated system settings");

        return getSettings();
    }

    private Map<String, String> loadKeyMap() {
        List<SystemSetting> rows = systemSettingRepository.findByKeyIn(ALL_KEYS);
        return rows.stream()
                .filter(s -> s.getKey() != null)
                .collect(Collectors.toMap(SystemSetting::getKey,
                        s -> s.getValue() == null ? "" : s.getValue(), (a, b) -> a));
    }

    private void upsert(String key, String value, String description, User admin) {
        SystemSetting setting = systemSettingRepository.findByKey(key).orElseGet(() -> {
            SystemSetting s = new SystemSetting();
            s.setKey(key);
            s.setDescription(description);
            return s;
        });
        setting.setValue(value);
        setting.setUpdatedBy(admin);
        if (setting.getDescription() == null || setting.getDescription().isBlank()) {
            setting.setDescription(description);
        }
        systemSettingRepository.save(setting);
    }

    private int parseIntOrDefault(String raw, int defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
