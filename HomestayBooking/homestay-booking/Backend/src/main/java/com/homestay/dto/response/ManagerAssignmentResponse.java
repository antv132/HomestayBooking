package com.homestay.dto.response;

import com.homestay.entity.ManagerPropertyAssignment;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/** FR-06 — Assignment history row for Admin SCR-49. */
@Data
public class ManagerAssignmentResponse {

    private UUID id;
    private UUID managerId;
    private String managerName;
    private String managerEmail;
    private String status;
    private LocalDateTime assignedAt;
    private UUID assignedById;
    private String assignedByName;

    public static ManagerAssignmentResponse fromEntity(ManagerPropertyAssignment mpa) {
        ManagerAssignmentResponse r = new ManagerAssignmentResponse();
        r.setId(mpa.getId());
        if (mpa.getManager() != null) {
            r.setManagerId(mpa.getManager().getId());
            r.setManagerName(mpa.getManager().getFullName());
            r.setManagerEmail(mpa.getManager().getEmail());
        }
        r.setStatus(mpa.getStatus() != null ? mpa.getStatus().name() : null);
        r.setAssignedAt(mpa.getAssignedAt());
        if (mpa.getAssignedBy() != null) {
            r.setAssignedById(mpa.getAssignedBy().getId());
            r.setAssignedByName(mpa.getAssignedBy().getFullName());
        }
        return r;
    }
}
