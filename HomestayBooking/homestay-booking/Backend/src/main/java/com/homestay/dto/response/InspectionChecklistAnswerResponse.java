package com.homestay.dto.response;

import com.homestay.entity.InspectionChecklistAnswer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InspectionChecklistAnswerResponse {
    private UUID id;
    private UUID itemId;
    private String code;
    private String label;
    private String icon;
    private boolean passed;

    public static InspectionChecklistAnswerResponse fromEntity(InspectionChecklistAnswer a) {
        return new InspectionChecklistAnswerResponse(
                a.getId(),
                a.getChecklistItem().getId(),
                a.getChecklistItem().getCode(),
                a.getChecklistItem().getLabel(),
                a.getChecklistItem().getIcon(),
                Boolean.TRUE.equals(a.getPassed())
        );
    }
}
