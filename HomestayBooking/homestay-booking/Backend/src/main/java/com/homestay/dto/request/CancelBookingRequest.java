package com.homestay.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelBookingRequest {
    /** Optional for customer cancel; required for manager cancel (validated in service). */
    @Size(max = 500, message = "Lý do hủy tối đa 500 ký tự")
    private String reason;

    /** Alias used by SCR-69 / docs (`cancelReason`). Mapped to {@link #reason}. */
    @Size(max = 500, message = "Lý do hủy tối đa 500 ký tự")
    private String cancelReason;

    public String resolvedReason() {
        if (cancelReason != null && !cancelReason.isBlank()) {
            return cancelReason.trim();
        }
        if (reason != null && !reason.isBlank()) {
            return reason.trim();
        }
        return null;
    }
}
