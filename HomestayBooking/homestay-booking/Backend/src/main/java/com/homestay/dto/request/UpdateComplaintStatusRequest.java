package com.homestay.dto.request;

import com.homestay.entity.Complaint;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateComplaintStatusRequest {

    @NotNull(message = "Status is required")
    private Complaint.Status status;

    private String resolutionNotes;
}
