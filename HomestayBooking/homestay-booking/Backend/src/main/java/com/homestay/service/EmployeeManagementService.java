package com.homestay.service;

import com.homestay.dto.request.AssignEmployeeRequest;
import com.homestay.dto.request.CreateEmployeeRequest;
import com.homestay.dto.request.ReassignEmployeeRequest;
import com.homestay.dto.request.UpdateEmployeeRequest;
import com.homestay.dto.request.UpdateEmployeeStatusRequest;
import com.homestay.dto.response.EmployeeSummaryResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.EmployeePropertyAssignment;
import com.homestay.entity.Property;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ConflictException;
import com.homestay.exception.ForbiddenException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.EmployeePropertyAssignmentRepository;
import com.homestay.repository.PropertyRepository;
import com.homestay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** SCR-39 / FR-20 — Manager + Admin employee management. */
@Service
@RequiredArgsConstructor
public class EmployeeManagementService {

    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmployeePropertyAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final ReportPropertyScopeValidator scopeValidator;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public PageResponse<EmployeeSummaryResponse> listByProperty(
            User actor, UUID propertyId, String search, int page, int size) {
        scopeValidator.validateManagerAccess(actor, propertyId);
        Pageable pageable = PageRequest.of(page, size);
        String normalizedSearch = normalizeSearch(search);
        Page<EmployeePropertyAssignment> result = assignmentRepository
                .findByPropertyIdAndStatusWithSearch(
                        propertyId, EmployeePropertyAssignment.Status.ACTIVE, normalizedSearch, pageable);
        return toPage(result, page, size);
    }

    /** Admin global list — optional property filter. */
    @Transactional(readOnly = true)
    public PageResponse<EmployeeSummaryResponse> listAdmin(
            User admin, UUID propertyId, String search, int page, int size) {
        requireAdmin(admin);
        Pageable pageable = PageRequest.of(page, size);
        String normalizedSearch = normalizeSearch(search);
        Page<EmployeePropertyAssignment> result = assignmentRepository
                .findActiveWithOptionalPropertyAndSearch(
                        propertyId,
                        EmployeePropertyAssignment.Status.ACTIVE,
                        normalizedSearch,
                        pageable);
        return toPage(result, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeSummaryResponse> listUnassigned(
            User actor, UUID propertyId, String search, int page, int size) {
        if (actor.getRole() != User.Role.ADMIN) {
            scopeValidator.validateManagerAccess(actor, propertyId);
        }
        Pageable pageable = PageRequest.of(page, size);
        String normalizedSearch = normalizeSearch(search);
        Page<User> result = assignmentRepository.findUnassignedEmployees(
                normalizedSearch,
                User.Role.EMPLOYEE,
                User.Status.ACTIVE,
                EmployeePropertyAssignment.Status.ACTIVE,
                pageable);
        List<EmployeeSummaryResponse> content = result.getContent().stream()
                .map(EmployeeSummaryResponse::fromUser)
                .toList();
        return new PageResponse<>(content, page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public EmployeeSummaryResponse assignEmployee(User actor, AssignEmployeeRequest request) {
        UUID propertyId = request.getPropertyId();
        UUID employeeId = request.getEmployeeId();
        scopeValidator.validateManagerAccess(actor, propertyId);

        Property property = requireActiveProperty(propertyId);
        User employee = requireActiveEmployeeUser(employeeId);

        if (assignmentRepository.existsByEmployeeIdAndPropertyIdAndStatus(
                employeeId, propertyId, EmployeePropertyAssignment.Status.ACTIVE)) {
            throw new ConflictException("Nhân viên đã được gán homestay này");
        }

        assignmentRepository.findFirstByEmployee_IdAndStatus(
                employeeId, EmployeePropertyAssignment.Status.ACTIVE).ifPresent(existing -> {
            if (!existing.getProperty().getId().equals(propertyId)) {
                throw new ConflictException("Nhân viên đã được gán homestay khác");
            }
        });

        EmployeePropertyAssignment epa = new EmployeePropertyAssignment();
        epa.setEmployee(employee);
        epa.setProperty(property);
        epa.setAssignedBy(actor);
        epa.setAssignedAt(LocalDateTime.now());
        epa.setStatus(EmployeePropertyAssignment.Status.ACTIVE);
        assignmentRepository.save(epa);

        activityLogService.log(actor, ActivityLogService.EMPLOYEE_ASSIGNED, "User", employeeId,
                "Assigned employee to property " + propertyId);

        return EmployeeSummaryResponse.fromAssignment(epa);
    }

    @Transactional
    public EmployeeSummaryResponse createEmployee(User actor, CreateEmployeeRequest request) {
        UUID propertyId = request.getPropertyId();
        scopeValidator.validateManagerAccess(actor, propertyId);

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email đã được sử dụng");
        }

        String fullName = request.getFullName().trim();
        if (fullName.length() < 2) {
            throw new BusinessException("Họ tên phải có ít nhất 2 ký tự");
        }

        Property property = requireActiveProperty(propertyId);

        String tempPassword = generateTempPassword();
        User employee = new User();
        employee.setFullName(fullName);
        employee.setEmail(email);
        employee.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        employee.setRole(User.Role.EMPLOYEE);
        employee.setStatus(User.Status.ACTIVE);
        employee.setPasswordHash(passwordEncoder.encode(tempPassword));
        userRepository.save(employee);

        EmployeePropertyAssignment epa = new EmployeePropertyAssignment();
        epa.setEmployee(employee);
        epa.setProperty(property);
        epa.setAssignedBy(actor);
        epa.setAssignedAt(LocalDateTime.now());
        epa.setStatus(EmployeePropertyAssignment.Status.ACTIVE);
        assignmentRepository.save(epa);

        sendInviteEmail(employee, tempPassword, property.getName());

        activityLogService.log(actor, ActivityLogService.EMPLOYEE_CREATED, "User", employee.getId(),
                "Created employee and assigned to property " + propertyId);

        return EmployeeSummaryResponse.fromAssignment(epa);
    }

    @Transactional
    public EmployeeSummaryResponse updateEmployee(
            User actor, UUID employeeId, UUID propertyId, UpdateEmployeeRequest request) {
        EmployeePropertyAssignment epa = requireManagedActiveAssignment(actor, employeeId, propertyId);
        User employee = epa.getEmployee();
        if (employee.getRole() != User.Role.EMPLOYEE) {
            throw new BusinessException("Chỉ có thể cập nhật tài khoản nhân viên");
        }

        String fullName = request.getFullName().trim();
        if (fullName.length() < 2) {
            throw new BusinessException("Họ tên phải có ít nhất 2 ký tự");
        }

        employee.setFullName(fullName);
        employee.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        userRepository.save(employee);

        activityLogService.log(actor, ActivityLogService.EMPLOYEE_UPDATED, "User", employeeId,
                "Updated employee profile");

        return EmployeeSummaryResponse.fromAssignment(epa);
    }

    /** Admin update — resolve ACTIVE assignment without Manager property scope. */
    @Transactional
    public EmployeeSummaryResponse updateEmployeeAdmin(
            User admin, UUID employeeId, UpdateEmployeeRequest request) {
        requireAdmin(admin);
        EmployeePropertyAssignment epa = requireActiveAssignment(employeeId);
        return updateEmployee(admin, employeeId, epa.getProperty().getId(), request);
    }

    @Transactional
    public EmployeeSummaryResponse updateEmployeeStatus(
            User actor, UUID employeeId, UUID propertyId, UpdateEmployeeStatusRequest request) {
        if (request.getStatus() != User.Status.ACTIVE && request.getStatus() != User.Status.SUSPENDED) {
            throw new BusinessException("Chỉ hỗ trợ chuyển trạng thái Đang hoạt động hoặc Tạm khóa");
        }

        EmployeePropertyAssignment epa = requireManagedActiveAssignment(actor, employeeId, propertyId);
        User employee = epa.getEmployee();
        if (employee.getRole() != User.Role.EMPLOYEE) {
            throw new BusinessException("Chỉ có thể đổi trạng thái tài khoản nhân viên");
        }

        if (employee.getStatus() == request.getStatus()) {
            return EmployeeSummaryResponse.fromAssignment(epa);
        }

        User.Status previous = employee.getStatus();
        employee.setStatus(request.getStatus());
        userRepository.save(employee);

        activityLogService.log(actor, ActivityLogService.EMPLOYEE_STATUS_CHANGED, "User", employeeId,
                "Status " + previous + " → " + request.getStatus());

        return EmployeeSummaryResponse.fromAssignment(epa);
    }

    @Transactional
    public EmployeeSummaryResponse updateEmployeeStatusAdmin(
            User admin, UUID employeeId, UpdateEmployeeStatusRequest request) {
        requireAdmin(admin);
        EmployeePropertyAssignment epa = requireActiveAssignment(employeeId);
        return updateEmployeeStatus(admin, employeeId, epa.getProperty().getId(), request);
    }

    @Transactional
    public EmployeeSummaryResponse reassignEmployee(
            User admin, UUID employeeId, ReassignEmployeeRequest request) {
        requireAdmin(admin);
        UUID newPropertyId = request.getPropertyId();
        Property newProperty = requireActiveProperty(newPropertyId);

        EmployeePropertyAssignment current = requireActiveAssignment(employeeId);
        if (current.getProperty().getId().equals(newPropertyId)) {
            throw new ConflictException("Nhân viên đã được gán homestay này");
        }

        UUID oldPropertyId = current.getProperty().getId();
        current.setStatus(EmployeePropertyAssignment.Status.INACTIVE);
        // Flush INACTIVE before INSERT ACTIVE — filtered unique index uq_epa_one_active_per_employee
        assignmentRepository.saveAndFlush(current);

        EmployeePropertyAssignment next = new EmployeePropertyAssignment();
        next.setEmployee(current.getEmployee());
        next.setProperty(newProperty);
        next.setAssignedBy(admin);
        next.setAssignedAt(LocalDateTime.now());
        next.setStatus(EmployeePropertyAssignment.Status.ACTIVE);
        assignmentRepository.save(next);

        activityLogService.log(admin, ActivityLogService.EMPLOYEE_REASSIGNED, "User", employeeId,
                "Reassigned from property " + oldPropertyId + " to " + newPropertyId);

        return EmployeeSummaryResponse.fromAssignment(next);
    }

    private EmployeePropertyAssignment requireManagedActiveAssignment(
            User actor, UUID employeeId, UUID propertyId) {
        scopeValidator.validateManagerAccess(actor, propertyId);
        return assignmentRepository.findByEmployee_IdAndProperty_IdAndStatus(
                employeeId, propertyId, EmployeePropertyAssignment.Status.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nhân viên chưa được gán homestay này hoặc không tồn tại"));
    }

    private EmployeePropertyAssignment requireActiveAssignment(UUID employeeId) {
        return assignmentRepository.findFirstByEmployee_IdAndStatus(
                employeeId, EmployeePropertyAssignment.Status.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nhân viên chưa có homestay đang gán hoặc không tồn tại"));
    }

    private Property requireActiveProperty(UUID propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy homestay"));
        if (property.getStatus() != Property.Status.ACTIVE) {
            throw new BusinessException("Homestay không ở trạng thái hoạt động");
        }
        return property;
    }

    private User requireActiveEmployeeUser(UUID employeeId) {
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên"));
        if (employee.getRole() != User.Role.EMPLOYEE) {
            throw new BusinessException("Người dùng không phải nhân viên");
        }
        if (employee.getStatus() != User.Status.ACTIVE) {
            throw new BusinessException("Chỉ có thể gán nhân viên đang hoạt động");
        }
        return employee;
    }

    private void sendInviteEmail(User employee, String tempPassword, String propertyName) {
        String body = "Xin chào " + employee.getFullName() + ",\n\n"
                + "Tài khoản nhân viên Homestay của bạn đã được tạo.\n"
                + "Homestay: " + propertyName + "\n"
                + "Email đăng nhập: " + employee.getEmail() + "\n"
                + "Mật khẩu tạm: " + tempPassword + "\n\n"
                + "Vui lòng đăng nhập và đổi mật khẩu sớm nhất có thể.\n"
                + "Nếu không dùng được mật khẩu tạm, hãy dùng Quên mật khẩu trên trang đăng nhập.\n\n"
                + "Trân trọng,\nĐội ngũ Homestay";
        emailService.trySendPlainText(
                employee.getEmail(),
                "[Homestay] Tài khoản nhân viên của bạn",
                body);
    }

    private static String generateTempPassword() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    private static void requireAdmin(User actor) {
        if (actor == null || actor.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException("Chỉ Admin được phép thực hiện thao tác này");
        }
    }

    private static PageResponse<EmployeeSummaryResponse> toPage(
            Page<EmployeePropertyAssignment> result, int page, int size) {
        List<EmployeeSummaryResponse> content = result.getContent().stream()
                .map(EmployeeSummaryResponse::fromAssignment)
                .toList();
        return new PageResponse<>(content, page, size, result.getTotalElements(), result.getTotalPages());
    }

    private static String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return search.trim();
    }
}
