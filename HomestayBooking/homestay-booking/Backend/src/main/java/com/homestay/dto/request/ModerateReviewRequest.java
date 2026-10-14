package com.homestay.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** FR-14 — Manager/Admin Hide or Show review. */
@Data
public class ModerateReviewRequest {

    /** PUBLISHED | HIDDEN */
    @NotBlank
    private String status;
}
