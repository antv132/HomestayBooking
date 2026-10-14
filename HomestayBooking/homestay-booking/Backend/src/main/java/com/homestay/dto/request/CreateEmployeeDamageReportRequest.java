package com.homestay.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** SCR-64 — Create Damage Report body from Employee. */
@Data
public class CreateEmployeeDamageReportRequest {

    @NotNull(message = "Phòng không được để trống")
    private UUID roomId;

    /** Optional — khi chọn đúng inspection FAILED chưa có báo cáo. */
    private UUID inspectionId;

    @NotEmpty(message = "Cần ít nhất một mục hư hại")
    @Valid
    private List<Item> items;

    @NotEmpty(message = "Cần ít nhất một ảnh minh chứng")
    @Valid
    private List<AttachmentRef> attachments;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
    private String notes;

    @Data
    public static class Item {
        @NotBlank(message = "Tên hư hại không được để trống")
        @Size(max = 200, message = "Tên hư hại tối đa 200 ký tự")
        private String name;

        @NotNull(message = "Phí ước tính không được để trống")
        @Positive(message = "Phí ước tính phải lớn hơn 0")
        private BigDecimal estimatedCost;
    }

    @Data
    public static class AttachmentRef {
        @NotBlank(message = "URL ảnh không được để trống")
        private String url;

        private String type;
    }
}
