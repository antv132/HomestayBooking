package com.homestay.dto.response;

import com.homestay.entity.Property;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** SCR-27 — Property selector item for assigned Manager properties. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignedPropertyResponse {

    private UUID id;
    private String name;
    private String address;
    private String status;

    public static AssignedPropertyResponse fromEntity(Property property) {
        return AssignedPropertyResponse.builder()
                .id(property.getId())
                .name(property.getName())
                .address(property.getAddress())
                .status(property.getStatus().name())
                .build();
    }
}
