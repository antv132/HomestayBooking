package com.homestay.repository;

import com.homestay.entity.RefreshToken;
import com.homestay.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    // Tìm token để validate khi client gửi lên
    Optional<RefreshToken> findByToken(String token);

    // Thu hồi tất cả token của user khi đổi mật khẩu hoặc logout all
    @Modifying
    @Query("UPDATE RefreshToken t SET t.revokedAt = CURRENT_TIMESTAMP WHERE t.user = :user AND t.revokedAt IS NULL")
    void revokeAllByUser(User user);
}
