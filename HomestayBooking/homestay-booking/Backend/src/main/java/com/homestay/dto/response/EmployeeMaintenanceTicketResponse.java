package com.homestay.dto.response;

import com.homestay.entity.MaintenanceTicket;
import com.homestay.entity.Room;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** SCR-61 — Employee maintenance ticket list item. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeMaintenanceTicketResponse {

    private String id;
    private String roomName;
    /** Ticket title (free text) — not a category enum. */
    private String issueType;
    private String description;
    private String status;
    private String resolutionNote;
    private LocalDateTime assignedAt;
    private LocalDateTime resolvedAt;

    public static EmployeeMaintenanceTicketResponse fromEntity(MaintenanceTicket ticket) {
        Room room = ticket.getRoom();
        String roomName = room != null ? room.getRoomNumber() : null;
        LocalDateTime assignedAt = ticket.getAssignedAt() != null
                ? ticket.getAssignedAt()
                : ticket.getCreatedAt();
        LocalDateTime resolvedAt = null;
        if (ticket.getStatus() == MaintenanceTicket.Status.RESOLVED
                || ticket.getStatus() == MaintenanceTicket.Status.CLOSED) {
            resolvedAt = ticket.getVerifiedAt() != null
                    ? ticket.getVerifiedAt()
                    : ticket.getUpdatedAt();
        }
        return EmployeeMaintenanceTicketResponse.builder()
                .id(ticket.getId().toString())
                .roomName(roomName)
                .issueType(ticket.getTitle())
                .description(ticket.getDescription() != null ? ticket.getDescription() : "")
                .status(ticket.getStatus().name())
                .resolutionNote(ticket.getWorkNote() != null ? ticket.getWorkNote() : ticket.getResolutionNote())
                .assignedAt(assignedAt)
                .resolvedAt(resolvedAt)
                .build();
    }
}
