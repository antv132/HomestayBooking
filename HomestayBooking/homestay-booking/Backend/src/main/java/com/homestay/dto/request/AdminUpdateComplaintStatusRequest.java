package com.homestay.dto.request;

import com.homestay.entity.Complaint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUpdateComplaintStatusRequest {

    @NotNull(message = "Trang thai la bat buoc")
    private Complaint.Status status;

    @Size(max = 2000, message = "Ghi chu giai quyet toi da 2000 ky tu")
    private String resolution;

    @Size(max = 2000, message = "Ghi chu giai quyet toi da 2000 ky tu")
    private String resolutionNotes;
}
