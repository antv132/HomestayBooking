package com.homestay.dto.response;

import com.homestay.entity.RoomStatusBlock;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** FR-05 T036 — active Maintenance / Out Of Service blocks for a room. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomStatusBlockResponse {

    private UUID id;
    private String status;
    private String startDate;
    private String endDate;
    private String reason;

    public static RoomStatusBlockResponse fromEntity(RoomStatusBlock block) {
        return new RoomStatusBlockResponse(
                block.getId(),
                block.getStatus().name(),
                block.getStartDate().toString(),
                block.getEndDate().toString(),
                block.getReason());
    }
}
