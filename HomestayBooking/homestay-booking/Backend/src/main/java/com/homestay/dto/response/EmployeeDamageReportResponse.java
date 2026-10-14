package com.homestay.dto.response;

import com.homestay.entity.DamageItem;
import com.homestay.entity.DamageReport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** SCR-63 — Employee damage report list item. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDamageReportResponse {

    private String id;
    private String bookingId;
    private String roomName;
    private String status;
    private List<Item> items;
    private int itemCount;
    private BigDecimal totalCost;
    private Boolean requiresAdminEscalation;
    private String note;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String name;
        private BigDecimal estimatedCost;
    }

    public static EmployeeDamageReportResponse fromEntity(DamageReport dr) {
        String roomName = dr.getInspection() != null && dr.getInspection().getRoom() != null
                ? dr.getInspection().getRoom().getRoomNumber()
                : null;

        List<Item> items = dr.getItems() == null ? List.of() : dr.getItems().stream()
                .map(EmployeeDamageReportResponse::toItem)
                .toList();

        BigDecimal totalCost = dr.getTotalEstimatedCost();
        if (totalCost == null) {
            totalCost = items.stream()
                    .map(Item::getEstimatedCost)
                    .filter(v -> v != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        String bookingId = dr.getBooking() != null && dr.getBooking().getId() != null
                ? dr.getBooking().getId().toString()
                : null;

        return EmployeeDamageReportResponse.builder()
                .id(dr.getId().toString())
                .bookingId(bookingId)
                .roomName(roomName)
                .status(dr.getStatus() != null ? dr.getStatus().name() : null)
                .items(items)
                .itemCount(items.size())
                .totalCost(totalCost)
                .requiresAdminEscalation(Boolean.TRUE.equals(dr.getRequiresAdminEscalation()))
                .note(dr.getNote())
                .createdAt(dr.getCreatedAt())
                .build();
    }

    private static Item toItem(DamageItem di) {
        return Item.builder()
                .name(di.getItemName())
                .estimatedCost(di.getEstimatedCost())
                .build();
    }
}
