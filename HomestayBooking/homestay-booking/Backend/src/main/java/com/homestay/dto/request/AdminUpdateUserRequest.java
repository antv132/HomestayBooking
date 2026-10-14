package com.homestay.dto.request;

import lombok.Data;

/**
 * Request DTO cho SCR-50/51 - Admin cap nhat user (partial update).
 * Ca 2 field OPTIONAL; parse/validate enum o service.
 */
@Data
public class AdminUpdateUserRequest {

    /** ADMIN | MANAGER | EMPLOYEE | CUSTOMER */
    private String role;

    /** INACTIVE | ACTIVE | SUSPENDED */
    private String status;
}