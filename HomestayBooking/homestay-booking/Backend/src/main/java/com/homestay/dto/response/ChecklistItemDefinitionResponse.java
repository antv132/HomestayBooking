package com.homestay.dto.response;

import com.homestay.entity.ChecklistItemDefinition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItemDefinitionResponse {
    private UUID id;
    private String code;
    private String label;
    private String icon;
    private Integer sortOrder;

    public static ChecklistItemDefinitionResponse fromEntity(ChecklistItemDefinition d) {
        return new ChecklistItemDefinitionResponse(
                d.getId(),
                d.getCode(),
                d.getLabel(),
                d.getIcon(),
                d.getSortOrder()
        );
    }
}
