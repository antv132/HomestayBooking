package com.homestay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingsResponse {

    private int depositPercentage;
    private int cancelTimeoutHours;
    private String systemName;
    private String supportEmail;
    private String bankAccountNumber;
    private String bankAccountName;
    private String bankName;
    private LocalDateTime updatedAt;
}
