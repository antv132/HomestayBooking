package com.homestay.enums;

/**
 * FR-17 complaint lifecycle. Mirrors {@link com.homestay.entity.Complaint.Status}
 * for validators/DTOs; entity retains nested enum for JPA compatibility.
 */
public enum ComplaintStatus {
    OPEN,
    INVESTIGATING,
    RESOLVED,
    CLOSED;

    public static ComplaintStatus fromEntity(com.homestay.entity.Complaint.Status status) {
        return ComplaintStatus.valueOf(status.name());
    }

    public com.homestay.entity.Complaint.Status toEntity() {
        return com.homestay.entity.Complaint.Status.valueOf(name());
    }
}
