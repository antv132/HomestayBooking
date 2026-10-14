package com.homestay.repository;

import com.homestay.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Tìm user theo email (dùng khi login)
    Optional<User> findByEmail(String email);

    // Tìm user theo Google ID (dùng khi đăng nhập bằng Google)
    Optional<User> findByGoogleId(String googleId);

    // Kiểm tra email đã tồn tại chưa (dùng khi đăng ký)
    boolean existsByEmail(String email);

    // Tìm kiếm customer cho manager (theo tên hoặc email)
    Page<User> findByRoleAndFullNameContainingIgnoreCaseOrRoleAndEmailContainingIgnoreCase(
            User.Role role1, String fullName,
            User.Role role2, String email,
            Pageable pageable);

    // Lấy tất cả customer theo status
    Page<User> findByRoleAndStatus(User.Role role, User.Status status, Pageable pageable);

    // Lấy tất cả customer (không lọc)
    Page<User> findByRole(User.Role role, Pageable pageable);

    java.util.List<User> findByRole(User.Role role);

    // SCR-45: đếm user theo role (Admin Dashboard KPI)
    long countByRole(User.Role role);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.role = :role AND " +
            "(:status IS NULL OR u.status = :status) AND " +
            "(:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(COALESCE(u.phone, '')) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findByRoleWithFilters(
            @org.springframework.data.repository.query.Param("role") User.Role role,
            @org.springframework.data.repository.query.Param("status") User.Status status,
            @org.springframework.data.repository.query.Param("search") String search,
            Pageable pageable);
}
