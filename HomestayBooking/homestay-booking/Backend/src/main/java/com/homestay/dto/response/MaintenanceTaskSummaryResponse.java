package com.homestay.dto.response;

import com.homestay.entity.MaintenanceTicket;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** SCR-41: Tóm tắt yêu cầu bảo trì cho Manager (dùng cả list + drawer chi tiết). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceTaskSummaryResponse {

    private UUID id;
    private UUID roomId;
    private String roomNumber;
    private UUID propertyId;
    private String propertyName;
    private UUID customerId;
    private String customerName;
    private String title;
    private String description;
    private List<String> photoUrls;
    private String status;
    private UUID assignedEmployeeId;
    private String assignedEmployeeName;
    private String resolutionNote;
    private LocalDateTime assignedAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;

    public static MaintenanceTaskSummaryResponse fromEntity(MaintenanceTicket t) {
        return new MaintenanceTaskSummaryResponse(
                t.getId(),
                t.getRoom().getId(),
                t.getRoom().getRoomNumber(),
                t.getRoom().getProperty().getId(),
                t.getRoom().getProperty().getName(),
                t.getCustomer().getId(),
                t.getCustomer().getFullName(),
                t.getTitle(),
                t.getDescription(),
                parsePhotoUrls(t.getPhotoUrls()),
                t.getStatus().name(),
                t.getAssignedEmployee() != null ? t.getAssignedEmployee().getId() : null,
                t.getAssignedEmployee() != null ? t.getAssignedEmployee().getFullName() : null,
                t.getStatus() == MaintenanceTicket.Status.CLOSED
                        ? t.getResolutionNote()
                        : (t.getWorkNote() != null ? t.getWorkNote() : t.getResolutionNote()),
                t.getAssignedAt(),
                t.getVerifiedAt(),
                t.getCreatedAt()
        );
    }

    // photoUrls lưu dạng chuỗi "[url1, url2]" (List.toString) — parse về danh sách.
    private static List<String> parsePhotoUrls(String raw) {
        List<String> photos = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return photos;
        }
        String s = raw.trim();
        if (s.startsWith("[") && s.endsWith("]")) {
            s = s.substring(1, s.length() - 1);
        }
        for (String part : s.split(",")) {
            String url = part.trim().replaceAll("^\"|\"$", "");
            if (!url.isEmpty()) {
                photos.add(url);
            }
        }
        return photos;
    }
}
