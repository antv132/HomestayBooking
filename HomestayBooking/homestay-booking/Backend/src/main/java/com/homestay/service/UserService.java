package com.homestay.service;

import com.homestay.dto.request.ChangePasswordRequest;
import com.homestay.dto.request.UpdateProfileRequest;
import com.homestay.dto.response.UserProfileResponse;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.RefreshTokenRepository;
import com.homestay.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.homestay.repository.BookingRepository bookingRepository;

    public UserService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       com.homestay.repository.BookingRepository bookingRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.bookingRepository = bookingRepository;
    }

    // Lấy thông tin profile của chính mình
    public UserProfileResponse getMyProfile(UUID userId) {
        User user = findUserById(userId);
        return UserProfileResponse.fromUser(user);
    }

    // SCR-11 — cập nhật hồ sơ (self-scope)
    @Transactional
    public UserProfileResponse updateMyProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        if (request.getFullName() != null) {
            String name = request.getFullName().trim();
            if (name.isBlank()) {
                throw new BusinessException("Họ tên không được để trống");
            }
            if (name.length() > 255) {
                throw new BusinessException("Họ tên tối đa 255 ký tự");
            }
            user.setFullName(name);
        }

        if (request.getPhone() != null) {
            String phone = request.getPhone().replaceAll("[\\s\\-()]", "");
            if (phone.isBlank()) {
                user.setPhone(null);
            } else {
                if (!phone.matches("^(\\+84|0)[0-9]{9,10}$")) {
                    throw new BusinessException("Số điện thoại không hợp lệ");
                }
                user.setPhone(phone);
            }
        }

        if (request.getAvatarUrl() != null) {
            String url = request.getAvatarUrl().trim();
            if (url.isBlank()) {
                user.setAvatarUrl(null);
            } else {
                if (url.length() > 512) {
                    throw new BusinessException("URL avatar tối đa 512 ký tự");
                }
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    throw new BusinessException("URL avatar phải bắt đầu bằng http:// hoặc https://");
                }
                user.setAvatarUrl(url);
            }
        }

        userRepository.save(user);
        return UserProfileResponse.fromUser(user);
    }

    // Đổi mật khẩu
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = findUserById(userId);

        // User đăng nhập Google không có password
        if (user.getPasswordHash() == null) {
            throw new BusinessException("Tài khoản Google không thể đổi mật khẩu theo cách này");
        }

        // Kiểm tra mật khẩu hiện tại
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Mật khẩu hiện tại không đúng");
        }

        // Kiểm tra confirm
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Mật khẩu mới và xác nhận mật khẩu không khớp");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Thu hồi tất cả refresh token để bắt đăng nhập lại
        refreshTokenRepository.revokeAllByUser(user);
    }

    // Helper
    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    // --- MANAGER API ---
    @Transactional(readOnly = true)
    public com.homestay.dto.response.PageResponse<com.homestay.dto.response.CustomerSummaryResponse> getAllCustomers(int page, int size, String statusStr, String search) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        User.Status status = null;
        if (statusStr != null && !statusStr.isBlank() && !statusStr.equalsIgnoreCase("ALL")) {
            status = User.Status.valueOf(statusStr.toUpperCase());
        }

        org.springframework.data.domain.Page<User> usersPage = userRepository.findByRoleWithFilters(User.Role.CUSTOMER, status, search != null && !search.isBlank() ? search : null, pageable);

        java.util.List<com.homestay.dto.response.CustomerSummaryResponse> content = usersPage.getContent().stream().map(u -> com.homestay.dto.response.CustomerSummaryResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .status(u.getStatus().name())
                .createdAt(u.getCreatedAt())
                .bookingCount(bookingRepository.countByCustomerId(u.getId()))
                .build()).toList();

        return new com.homestay.dto.response.PageResponse<>(content, usersPage.getNumber(), usersPage.getSize(), usersPage.getTotalElements(), usersPage.getTotalPages());
    }

    @Transactional(readOnly = true)
    public com.homestay.dto.response.CustomerDetailResponse getCustomerDetail(UUID id) {
        User user = findUserById(id);
        if (user.getRole() != User.Role.CUSTOMER) {
            throw new BusinessException("Người dùng không phải là Customer");
        }

        org.springframework.data.domain.Page<com.homestay.entity.Booking> bookingsPage = 
            bookingRepository.findByCustomerId(user.getId(), 
                org.springframework.data.domain.PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("createdAt").descending()));

        java.util.List<com.homestay.dto.response.CustomerDetailResponse.BookingSummary> recentBookings = 
            bookingsPage.getContent().stream().map(b -> 
                com.homestay.dto.response.CustomerDetailResponse.BookingSummary.builder()
                    .id(b.getId())
                    .roomNumber(b.getRoom().getRoomNumber())
                    .propertyName(b.getRoom().getProperty().getName())
                    .checkInDate(b.getCheckInDate().toString())
                    .checkOutDate(b.getCheckOutDate().toString())
                    .totalAmount(b.getTotalAmount() != null ? b.getTotalAmount().longValue() : 0L)
                    .status(b.getStatus().name())
                    .build()
            ).toList();

        return com.homestay.dto.response.CustomerDetailResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus().name())
                .createdAt(user.getCreatedAt())
                .bookingCount(bookingRepository.countByCustomerId(user.getId()))
                .recentBookings(recentBookings)
                .build();
    }

    @Transactional
    public void updateCustomerStatus(UUID id, String statusStr) {
        User user = findUserById(id);
        if (user.getRole() != User.Role.CUSTOMER) {
            throw new BusinessException("Chỉ có thể thay đổi trạng thái của Customer");
        }
        User.Status newStatus = User.Status.valueOf(statusStr.toUpperCase());
        user.setStatus(newStatus);
        userRepository.save(user);
    }
}
