package com.homestay.dto.request;

import lombok.Data;

@Data
public class UpdateSystemSettingsRequest {

    private Integer depositPercentage;
    private Integer cancelTimeoutHours;
    private String systemName;
    private String supportEmail;
    private String bankAccountNumber;
    private String bankAccountName;
    private String bankName;
}
