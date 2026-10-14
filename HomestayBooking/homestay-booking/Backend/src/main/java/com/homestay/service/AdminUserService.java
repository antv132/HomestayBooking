package com.homestay.service;

import com.homestay.dto.request.AdminCreateManagerRequest;
import com.homestay.dto.request.AdminUpdateManagerRequest;
import com.homestay.dto.request.AdminUpdateUserRequest;
import com.homestay.dto.request.UpdateCustomerStatusRequest;
import com.homestay.dto.response.AdminCustomerBookingSummaryResponse;
import com.homestay.dto.response.AdminUserResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.ManagerPropertyAssignment;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ConflictException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import com.homestay.repository.PaymentRepository;
import com.homestay.repository.RefreshTokenRepository;
import com.homestay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SCR-50/51 - Manager/Customer Directory (Admin).
 * Reuse UserRepository.findByRoleWithFilters.
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final ManagerPropertyAssignmentRepository managerAssignmentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ActivityLogService activityLogService;
    private final PasswordEncoder passwordEncoder;

    /** SCR-50 — Admin tạo tài khoản Manager (ACTIVE ngay, không cần OTP). */
    @Transactional
    public AdminUserResponse createManager(AdminCreateManagerRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email đã được sử dụng. Vui lòng dùng email khác.");
        }

        User user = new User();
        user.setFullName(req.getFullName().trim());
        user.setEmail(email);
        user.setPhone(req.getPhone() != null && !req.getPhone().isBlank() ? req.getPhone().trim() : null);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRole(User.Role.MANAGER);
        user.setStatus(User.Status.ACTIVE);

        User saved = userRepository.save(user);
        return AdminUserResponse.fromEntity(saved, 0, null, null);
    }

    /** SCR-50 — Admin sửa tài khoản Manager (hồ sơ + mật khẩu tùy chọn + trạng thái). */
    @Transactional
    public AdminUserResponse updateManager(UUID id, AdminUpdateManagerRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với ID: " + id));
        if (user.getRole() != User.Role.MANAGER) {
            throw new BusinessException("Chỉ có thể sửa tài khoản Manager qua endpoint này");
        }

        String email = req.getEmail().trim().toLowerCase();
        if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(email)) {
            throw new BusinessException("Email đã được sử dụng. Vui lòng dùng email khác.");
        }

        user.setFullName(req.getFullName().trim());
        user.setEmail(email);
        user.setPhone(req.getPhone() != null && !req.getPhone().isBlank() ? req.getPhone().trim() : null);

        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            if (req.getPassword().length() < 8 || req.getPassword().length() > 100) {
                throw new BusinessException("Mật khẩu phải từ 8 đến 100 ký tự");
            }
            user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        }

        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            User.Status status = parseStatus(req.getStatus());
            if (status == null || (status != User.Status.ACTIVE && status != User.Status.INACTIVE)) {
                throw new BusinessException("Chỉ hỗ trợ trạng thái Đang hoạt động hoặc Vô hiệu hóa");
            }
            user.setStatus(status);
        }

        User saved = userRepository.save(user);
        return toDirectoryResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> listUsers(String role, String status, String keyword, Pageable pageable) {
        User.Role roleFilter = parseRole(role);
        if (roleFilter == null) {
            throw new BusinessException("Role không hợp lệ");
        }
        if (roleFilter != User.Role.MANAGER && roleFilter != User.Role.CUSTOMER) {
            throw new BusinessException("Chỉ hỗ trợ lọc Manager hoặc Customer");
        }

        User.Status statusFilter = parseStatus(status);
        if (status != null && !status.isBlank() && statusFilter == null) {
            throw new BusinessException("Trạng thái không hợp lệ");
        }
        String search = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        Page<User> page = userRepository.findByRoleWithFilters(roleFilter, statusFilter, search, pageable);

        Map<UUID, Integer> assignedCounts = Map.of();
        Map<UUID, Long> bookingCounts = Map.of();
        Map<UUID, BigDecimal> spendTotals = Map.of();

        if (!page.isEmpty()) {
            if (roleFilter == User.Role.MANAGER) {
                assignedCounts = loadManagerAssignmentCounts(page.getContent());
            } else {
                bookingCounts = loadCustomerBookingCounts(page.getContent());
                spendTotals = loadCustomerSpendTotals(page.getContent());
            }
        }

        Map<UUID, Integer> managerCounts = assignedCounts;
        Map<UUID, Long> customerBookings = bookingCounts;
        Map<UUID, BigDecimal> customerSpend = spendTotals;

        List<AdminUserResponse> content = page.getContent().stream()
                .map(u -> {
                    if (roleFilter == User.Role.MANAGER) {
                        return AdminUserResponse.fromEntity(
                                u, managerCounts.getOrDefault(u.getId(), 0), null, null);
                    }
                    return AdminUserResponse.fromEntity(
                            u,
                            null,
                            customerBookings.getOrDefault(u.getId(), 0L),
                            customerSpend.getOrDefault(u.getId(), BigDecimal.ZERO));
                })
                .toList();

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getById(UUID id) {
        User user = requireDirectoryUser(id);
        return toDirectoryResponse(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminCustomerBookingSummaryResponse> listCustomerBookings(
            UUID customerId, int page, int size) {
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với ID: " + customerId));
        if (user.getRole() != User.Role.CUSTOMER) {
            throw new BusinessException("Chỉ hỗ trợ xem lịch sử đặt phòng của Customer");
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 20);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<com.homestay.entity.Booking> result =
                bookingRepository.findByCustomerIdWithDetails(customerId, pageable);

        List<AdminCustomerBookingSummaryResponse> content = result.getContent().stream()
                .map(AdminCustomerBookingSummaryResponse::fromEntity)
                .toList();

        return new PageResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional
    public AdminUserResponse updateUser(UUID id, AdminUpdateUserRequest req, User actor) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với ID: " + id));

        if (user.getRole() == User.Role.ADMIN) {
            throw new BusinessException("Không thể cập nhật tài khoản Admin");
        }
        if (req.getRole() != null && !req.getRole().isBlank()) {
            throw new BusinessException("Không được đổi role qua Directory. Chỉ cập nhật trạng thái.");
        }
        if (user.getRole() != User.Role.MANAGER && user.getRole() != User.Role.CUSTOMER) {
            throw new BusinessException("Directory chỉ hỗ trợ cập nhật Manager hoặc Customer");
        }

        if (req.getStatus() == null || req.getStatus().isBlank()) {
            throw new BusinessException("Cần cung cấp trạng thái mới");
        }
        User.Status status = parseStatus(req.getStatus());
        if (status == null) {
            throw new BusinessException("Trạng thái không hợp lệ");
        }

        if (user.getRole() == User.Role.CUSTOMER) {
            return applyCustomerStatus(user, status, actor);
        }

        // FR-06 Manager directory — ACTIVE ↔ INACTIVE only
        if (status != User.Status.ACTIVE && status != User.Status.INACTIVE) {
            throw new BusinessException("Chỉ hỗ trợ trạng thái Đang hoạt động hoặc Vô hiệu hóa");
        }

        user.setStatus(status);
        User saved = userRepository.save(user);
        return toDirectoryResponse(saved);
    }

    /** FR-09 SCR-51 — PATCH status ACTIVE ↔ SUSPENDED. */
    @Transactional
    public AdminUserResponse updateCustomerStatus(UUID id, UpdateCustomerStatusRequest req, User actor) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với ID: " + id));
        if (user.getRole() != User.Role.CUSTOMER) {
            throw new BusinessException("Chỉ hỗ trợ cập nhật trạng thái tài khoản Customer");
        }
        if (req.getStatus() == null || req.getStatus().isBlank()) {
            throw new BusinessException("Cần cung cấp trạng thái mới");
        }
        User.Status status = parseStatus(req.getStatus());
        if (status == null) {
            throw new BusinessException("Trạng thái không hợp lệ");
        }
        return applyCustomerStatus(user, status, actor);
    }

    private AdminUserResponse applyCustomerStatus(User user, User.Status newStatus, User actor) {
        if (user.getStatus() == User.Status.INACTIVE) {
            throw new ConflictException(
                    "Không thể Suspend/Activate tài khoản chưa xác thực email (INACTIVE)");
        }
        if (newStatus != User.Status.ACTIVE && newStatus != User.Status.SUSPENDED) {
            throw new BusinessException("Customer chỉ hỗ trợ trạng thái Active hoặc Suspended");
        }
        if (user.getStatus() != User.Status.ACTIVE && user.getStatus() != User.Status.SUSPENDED) {
            throw new ConflictException("Chỉ có thể đổi trạng thái khi tài khoản đang Active hoặc Suspended");
        }
        if (user.getStatus() == newStatus) {
            return toDirectoryResponse(user);
        }

        User.Status previous = user.getStatus();
        user.setStatus(newStatus);
        User saved = userRepository.save(user);

        if (newStatus == User.Status.SUSPENDED) {
            refreshTokenRepository.revokeAllByUser(saved);
        }

        String action = newStatus == User.Status.SUSPENDED
                ? ActivityLogService.USER_SUSPENDED
                : ActivityLogService.USER_ACTIVATED;
        String details = previous.name() + " → " + newStatus.name();
        activityLogService.log(actor, action, "User", saved.getId(), details);

        return toDirectoryResponse(saved);
    }

    private User requireDirectoryUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với ID: " + id));
        if (user.getRole() == User.Role.ADMIN) {
            throw new BusinessException("Không thể xem chi tiết tài khoản Admin qua Directory");
        }
        return user;
    }

    private AdminUserResponse toDirectoryResponse(User user) {
        if (user.getRole() == User.Role.MANAGER) {
            int assigned = (int) managerAssignmentRepository.countByManager_IdAndStatus(
                    user.getId(), ManagerPropertyAssignment.Status.ACTIVE);
            return AdminUserResponse.fromEntity(user, assigned, null, null);
        }
        if (user.getRole() == User.Role.CUSTOMER) {
            long bookings = bookingRepository.countByCustomerId(user.getId());
            BigDecimal spend = paymentRepository.sumPaidAmountByCustomerId(user.getId());
            return AdminUserResponse.fromEntity(user, null, bookings, spend != null ? spend : BigDecimal.ZERO);
        }
        return AdminUserResponse.fromEntity(user);
    }

    private Map<UUID, Integer> loadManagerAssignmentCounts(List<User> managers) {
        List<UUID> ids = managers.stream().map(User::getId).toList();
        Map<UUID, Integer> map = new HashMap<>();
        for (Object[] row : managerAssignmentRepository.countActiveAssignmentsByManagerIds(
                ids, ManagerPropertyAssignment.Status.ACTIVE)) {
            map.put((UUID) row[0], ((Number) row[1]).intValue());
        }
        return map;
    }

    private Map<UUID, Long> loadCustomerBookingCounts(List<User> customers) {
        List<UUID> ids = customers.stream().map(User::getId).toList();
        Map<UUID, Long> map = new HashMap<>();
        for (Object[] row : bookingRepository.countBookingsByCustomerIds(ids)) {
            map.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }

    private Map<UUID, BigDecimal> loadCustomerSpendTotals(List<User> customers) {
        List<UUID> ids = customers.stream().map(User::getId).toList();
        Map<UUID, BigDecimal> map = new HashMap<>();
        for (Object[] row : paymentRepository.sumPaidAmountByCustomerIds(ids)) {
            map.put((UUID) row[0], (BigDecimal) row[1]);
        }
        return map;
    }

    private User.Role parseRole(String role) {
        if (role == null || role.isBlank()) return null;
        try {
            return User.Role.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private User.Status parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return User.Status.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
