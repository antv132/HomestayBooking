package com.homestay.exception;

import com.homestay.dto.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleValidationErrors_returnsFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("booking", "email", "Email không hợp lệ");
        
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ApiResponse<Map<String, String>>> response = handler.handleValidationErrors(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Dữ liệu không hợp lệ", response.getBody().getMessage());
        assertTrue(response.getBody().getData().containsKey("email"));
        assertEquals("Email không hợp lệ", response.getBody().getData().get("email"));
    }

    @Test
    void handleTypeMismatch_returnsBadRequest() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);

        ResponseEntity<ApiResponse<Void>> response = handler.handleTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Định dạng dữ liệu không hợp lệ"));
    }

    @Test
    void handleNotFound_returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Booking không tồn tại");

        ResponseEntity<ApiResponse<Void>> response = handler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Booking không tồn tại", response.getBody().getMessage());
    }

    @Test
    void handleAccountNotVerified_returns403WithErrorCode() {
        AccountNotVerifiedException ex = new AccountNotVerifiedException("test@example.com");

        ResponseEntity<ApiResponse<Map<String, String>>> response = handler.handleAccountNotVerified(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("ACCOUNT_INACTIVE", response.getBody().getData().get("errorCode"));
        assertEquals("test@example.com", response.getBody().getData().get("email"));
    }

    @Test
    void handleOtpExpired_returns410Gone() {
        OtpExpiredException ex = new OtpExpiredException("OTP đã hết hạn");

        ResponseEntity<ApiResponse<Void>> response = handler.handleOtpExpired(ex);

        assertEquals(HttpStatus.GONE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("OTP đã hết hạn", response.getBody().getMessage());
    }

    @Test
    void handleDataIntegrity_roomNumberConstraint_customMessage() {
        DataIntegrityViolationException ex = mock(DataIntegrityViolationException.class);
        Exception cause = new Exception("constraint [uq_room_number_property]");
        when(ex.getMostSpecificCause()).thenReturn(cause);

        ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrity(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("Số phòng đã tồn tại"));
    }

    @Test
    void handleDataIntegrity_employeeConstraint_customMessage() {
        DataIntegrityViolationException ex = mock(DataIntegrityViolationException.class);
        Exception cause = new Exception("constraint [uq_epa_one_active_per_employee]");
        when(ex.getMostSpecificCause()).thenReturn(cause);

        ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrity(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("Nhân viên đã có homestay"));
    }

    @Test
    void handleDataIntegrity_genericConstraint_defaultMessage() {
        DataIntegrityViolationException ex = mock(DataIntegrityViolationException.class);
        Exception cause = new Exception("some other constraint violation");
        when(ex.getMostSpecificCause()).thenReturn(cause);

        ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrity(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Dữ liệu bị trùng lặp", response.getBody().getMessage());
    }

    @Test
    void handleBusiness_returnsBadRequest() {
        BusinessException ex = new BusinessException("Phòng đã được đặt trong thời gian này");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Phòng đã được đặt trong thời gian này", response.getBody().getMessage());
    }

    @Test
    void handleConflict_returns409() {
        ConflictException ex = new ConflictException("Xung đột dữ liệu");

        ResponseEntity<ApiResponse<Void>> response = handler.handleConflict(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Xung đột dữ liệu", response.getBody().getMessage());
    }

    @Test
    void handleForbidden_returns403() {
        ForbiddenException ex = new ForbiddenException("Bạn không có quyền xem thông tin này");

        ResponseEntity<ApiResponse<Void>> response = handler.handleForbidden(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Bạn không có quyền xem thông tin này", response.getBody().getMessage());
    }

    @Test
    void handleAccessDenied_returns403() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Bạn không có quyền thực hiện thao tác này", response.getBody().getMessage());
    }

    @Test
    void handleIllegalArgument_returnsBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Tham số không hợp lệ");

        ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Tham số không hợp lệ", response.getBody().getMessage());
    }

    @Test
    void handleGeneral_returns500() {
        Exception ex = new RuntimeException("Unexpected error");

        ResponseEntity<ApiResponse<Void>> response = handler.handleGeneral(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Lỗi hệ thống, vui lòng thử lại sau", response.getBody().getMessage());
    }
}
