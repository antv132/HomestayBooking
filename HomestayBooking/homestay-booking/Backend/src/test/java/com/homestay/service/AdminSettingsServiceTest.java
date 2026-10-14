package com.homestay.service;

import com.homestay.dto.request.UpdateSystemSettingsRequest;
import com.homestay.dto.response.SystemSettingsResponse;
import com.homestay.entity.SystemSetting;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.repository.SystemSettingRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSettingsServiceTest {

    @Mock SystemSettingRepository systemSettingRepository;
    @Mock ActivityLogService activityLogService;

    private AdminSettingsService service;
    private User admin;

    @BeforeEach
    void setUp() {
        service = new AdminSettingsService(systemSettingRepository, activityLogService);
        admin = TestFixtures.user(User.Role.ADMIN);
    }

    @Test
    void getSettings_returnsDefaults_whenNoRows() {
        when(systemSettingRepository.findByKeyIn(any())).thenReturn(List.of());
        SystemSettingsResponse res = service.getSettings();
        assertEquals(40, res.getDepositPercentage());
        assertEquals(24, res.getCancelTimeoutHours());
    }

    @Test
    void getSettings_returnsStoredValues() {
        SystemSetting dep = new SystemSetting();
        dep.setKey("DEPOSIT_PERCENTAGE");
        dep.setValue("30");
        SystemSetting cancel = new SystemSetting();
        cancel.setKey("CANCEL_TIMEOUT_HOURS");
        cancel.setValue("48");
        when(systemSettingRepository.findByKeyIn(any())).thenReturn(List.of(dep, cancel));

        SystemSettingsResponse res = service.getSettings();
        assertEquals(30, res.getDepositPercentage());
        assertEquals(48, res.getCancelTimeoutHours());
    }

    @Test
    void getDepositPercentage_returnsFraction() {
        SystemSetting dep = new SystemSetting();
        dep.setKey("DEPOSIT_PERCENTAGE");
        dep.setValue("25");
        when(systemSettingRepository.findByKey("DEPOSIT_PERCENTAGE")).thenReturn(Optional.of(dep));

        BigDecimal pct = service.getDepositPercentage();
        assertEquals(new BigDecimal("0.25"), pct);
    }

    @Test
    void getDepositPercentage_returnsDefault_whenMissing() {
        when(systemSettingRepository.findByKey("DEPOSIT_PERCENTAGE")).thenReturn(Optional.empty());
        BigDecimal pct = service.getDepositPercentage();
        assertEquals(new BigDecimal("0.4"), pct);
    }

    @Test
    void updateSettings_depositBelow10_throws() {
        UpdateSystemSettingsRequest req = new UpdateSystemSettingsRequest();
        req.setDepositPercentage(5);
        assertThrows(BusinessException.class, () -> service.updateSettings(req, admin));
    }

    @Test
    void updateSettings_depositAbove50_throws() {
        UpdateSystemSettingsRequest req = new UpdateSystemSettingsRequest();
        req.setDepositPercentage(60);
        assertThrows(BusinessException.class, () -> service.updateSettings(req, admin));
    }

    @Test
    void updateSettings_deposit40_ok() {
        UpdateSystemSettingsRequest req = new UpdateSystemSettingsRequest();
        req.setDepositPercentage(40);
        SystemSetting dep = new SystemSetting();
        dep.setKey("DEPOSIT_PERCENTAGE");
        dep.setValue("40");
        when(systemSettingRepository.findByKey("DEPOSIT_PERCENTAGE")).thenReturn(Optional.of(dep));
        when(systemSettingRepository.save(any())).thenReturn(dep);
        when(systemSettingRepository.findByKeyIn(any())).thenReturn(List.of(dep));

        SystemSettingsResponse res = service.updateSettings(req, admin);
        assertEquals(40, res.getDepositPercentage());
        verify(activityLogService).log(any(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void updateSettings_invalidEmail_throws() {
        UpdateSystemSettingsRequest req = new UpdateSystemSettingsRequest();
        req.setSupportEmail("not-an-email");
        assertThrows(BusinessException.class, () -> service.updateSettings(req, admin));
    }

    @Test
    void updateSettings_validEmail_ok() {
        UpdateSystemSettingsRequest req = new UpdateSystemSettingsRequest();
        req.setSupportEmail("support@example.com");
        SystemSetting emailSetting = new SystemSetting();
        emailSetting.setKey("SUPPORT_EMAIL");
        emailSetting.setValue("support@example.com");
        when(systemSettingRepository.findByKey("SUPPORT_EMAIL")).thenReturn(Optional.of(emailSetting));
        when(systemSettingRepository.save(any())).thenReturn(emailSetting);
        when(systemSettingRepository.findByKeyIn(any())).thenReturn(List.of(emailSetting));

        SystemSettingsResponse res = service.updateSettings(req, admin);
        verify(activityLogService).log(any(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void updateSettings_emptyEmail_allowed() {
        UpdateSystemSettingsRequest req = new UpdateSystemSettingsRequest();
        req.setSupportEmail("");
        SystemSetting emailSetting = new SystemSetting();
        emailSetting.setKey("SUPPORT_EMAIL");
        emailSetting.setValue("");
        when(systemSettingRepository.findByKey("SUPPORT_EMAIL")).thenReturn(Optional.of(emailSetting));
        when(systemSettingRepository.save(any())).thenReturn(emailSetting);
        when(systemSettingRepository.findByKeyIn(any())).thenReturn(List.of(emailSetting));

        service.updateSettings(req, admin);
        verify(systemSettingRepository).save(any());
    }
}
