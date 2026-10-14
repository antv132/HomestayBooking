package com.homestay.dto.response;

import com.homestay.entity.EmployeePropertyAssignment;
import com.homestay.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class EmployeeSummaryResponse {

    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private User.Status status;
    private UUID propertyId;
    private String propertyName;
    private LocalDateTime assignedAt;
    private EmployeePropertyAssignment.Status assignmentStatus;

    public static EmployeeSummaryResponse fromAssignment(EmployeePropertyAssignment epa) {
        User employee = epa.getEmployee();
        return EmployeeSummaryResponse.builder()
                .id(employee.getId())
                .fullName(employee.getFullName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .avatarUrl(employee.getAvatarUrl())
                .status(employee.getStatus())
                .propertyId(epa.getProperty().getId())
                .propertyName(epa.getProperty().getName())
                .assignedAt(epa.getAssignedAt())
                .assignmentStatus(epa.getStatus())
                .build();
    }

    public static EmployeeSummaryResponse fromUser(User employee) {
        return EmployeeSummaryResponse.builder()
                .id(employee.getId())
                .fullName(employee.getFullName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .avatarUrl(employee.getAvatarUrl())
                .status(employee.getStatus())
                .build();
    }
}
