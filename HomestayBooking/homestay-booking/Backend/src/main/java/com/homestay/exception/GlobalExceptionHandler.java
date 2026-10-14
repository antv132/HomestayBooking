package com.homestay.exception;

import com.homestay.dto.response.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

// Xử lý tập trung tất cả exception trong app
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Lỗi validate @NotBlank, @Email, @Size, ...
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });

        ApiResponse<Map<String, String>> response = new ApiResponse<>(
                false, "Dữ liệu không hợp lệ", errors);
        return ResponseEntity.badRequest().body(response);
    }

    // Xử lý lỗi convert data type, vd UUID không hợp lệ
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(Exception ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Định dạng dữ liệu không hợp lệ (ví dụ: ID sai định dạng)"));
    }

    // Không tìm thấy resource (room, booking, user, ...)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // Tài khoản chưa xác thực email → 403 với errorCode + email để frontend redirect
    @ExceptionHandler(AccountNotVerifiedException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleAccountNotVerified(AccountNotVerifiedException ex) {
        Map<String, String> data = new HashMap<>();
        data.put("errorCode", "ACCOUNT_INACTIVE");
        data.put("email", ex.getEmail());
        ApiResponse<Map<String, String>> response = new ApiResponse<>(false, ex.getMessage(), data);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // OTP hết hạn → 410 Gone (phân biệt với OTP sai 400)
    @ExceptionHandler(OtpExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleOtpExpired(OtpExpiredException ex) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // Lỗi vi phạm constraint DB (unique, check, FK...)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String message = "Dữ liệu bị trùng lặp";
        String causeMsg = ex.getMostSpecificCause().getMessage();
        // Log nguyên nhân gốc để chẩn đoán (không expose chi tiết DB cho client)
        System.err.println("[DataIntegrityViolation] " + causeMsg);
        String cause = causeMsg == null ? "" : causeMsg.toLowerCase();
        if (cause.contains("uq_room_number_property")
                || cause.contains("uk_rooms_room_number_property_id")
                || cause.contains("uq_rooms_property_room_number_active")) {
            message = "Số phòng đã tồn tại trong property này. Vui lòng chọn số phòng khác.";
        } else if (cause.contains("uq_epa_one_active_per_employee")) {
            message = "Nhân viên đã có homestay đang gán. Hãy chuyển (reassign) thay vì gán thêm.";
        } else if (cause.contains("uq_notifications_dedupe_key")
                || (cause.contains("duplicate key") && cause.contains("dbo.notifications"))) {
            message = "Yêu cầu đã được xử lý trước đó. Vui lòng tải lại trang để xem trạng thái mới nhất.";
        } else if (cause.contains("uk_invoices_booking_type")
                || (cause.contains("duplicate key") && cause.contains("dbo.invoices"))) {
            message = "Hóa đơn cho booking này đã tồn tại, không thể tạo trùng.";
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(message));
    }

    // Lỗi logic nghiệp vụ (đặt phòng trùng, cọc đã thanh toán, ...)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // Lỗi không phân quyền
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // Spring Security @PreAuthorize / role mismatch → 403 (not 500)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Bạn không có quyền thực hiện thao tác này"));
    }

    // Lỗi logic đơn giản từ service (IllegalArgumentException)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }

    // Lỗi không mong đợi - chỉ log, không expose chi tiết cho client
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        ex.printStackTrace(); // Chỉ dùng cho dev, production nên dùng logger
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Lỗi hệ thống, vui lòng thử lại sau"));
    }
}
