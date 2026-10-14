package com.homestay.dto.response;

import com.homestay.entity.MaintenanceTicket;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class MaintenanceTicketResponse {
    private UUID id;
    private UUID customerId;
    private String customerName;
    private UUID roomId;
    private String roomName;
    private String title;
    private String description;
    private String photoUrls;
    private String status;
    private String resolutionNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MaintenanceTicketResponse fromEntity(MaintenanceTicket ticket) {
        MaintenanceTicketResponse response = new MaintenanceTicketResponse();
        response.setId(ticket.getId());
        response.setCustomerId(ticket.getCustomer().getId());
        response.setCustomerName(ticket.getCustomer().getFullName());
        response.setRoomId(ticket.getRoom().getId());
        response.setRoomName(ticket.getRoom().getRoomNumber());
        response.setTitle(ticket.getTitle());
        response.setDescription(ticket.getDescription());
        response.setPhotoUrls(ticket.getPhotoUrls());
        response.setStatus(ticket.getStatus().name());
        response.setResolutionNote(ticket.getResolutionNote());
        response.setCreatedAt(ticket.getCreatedAt());
        response.setUpdatedAt(ticket.getUpdatedAt());
        return response;
    }
}
