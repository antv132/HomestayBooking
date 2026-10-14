package com.homestay.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.homestay.dto.request.*;
import com.homestay.dto.response.AuthResponse;
import com.homestay.entity.RefreshToken;
import com.homestay.entity.User;
import com.homestay.exception.AccountNotVerifiedException;
import com.homestay.exception.BusinessException;
import com.homestay.exception.OtpExpiredException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.RefreshTokenRepository;
import com.homestay.repository.UserRepository;
import com.homestay.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /** FR-002: OTP TTL 15 minutes. */
    static final int OTP_TTL_MINUTES = 15;
    /** FR-002: max OTP sends per email per rolling hour. */
    static final int OTP_MAX_SENDS_PER_HOUR = 3;

    @Value("${app.google.client-id}")
    private String googleClientId;

    /** Prefer MAIL_USERNAME so From matches the SMTP authenticated account (required by Gmail). */
    @Value("${MAIL_USERNAME:${spring.mail.username:}}")
    private String mailFrom;

    /** FR-007: refresh token TTL — default 7 days (001 Assumptions). */
    @Value("${app.jwt.refresh-expiration-days:7}")
    private long refreshExpirationDays;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JavaMailSender mailSender;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.mailSender = mailSender;
    }

    // ── Đăng ký bằng email ────────────────────────────────────────────────────

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email đã được đăng ký. Vui lòng dùng email khác.");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.CUSTOMER);
        user.setStatus(User.Status.INACTIVE);

        issueAndSendOtp(user, "Xác thực tài khoản");
        userRepository.save(user);
    }

    @Transactional
    public void verifyOtp(OtpVerifyRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

        // Staff INACTIVE is Admin-activated only — OTP must not self-activate (001 edge case).
        if (isStaffRole(user.getRole())) {
            throw new BusinessException(
                    "Tài khoản nhân sự chưa được kích hoạt. Vui lòng liên hệ Admin.");
        }

        validateOtp(user, request.getOtpCode());

        user.setStatus(User.Status.ACTIVE);
        clearOtp(user);
        userRepository.save(user);
    }

    @Transactional
    public void resendOtp(ResendOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

        if (user.getStatus() == User.Status.ACTIVE) {
            throw new BusinessException("Tài khoản đã được kích hoạt rồi");
        }

        issueAndSendOtp(user, "Gửi lại mã xác thực");
        userRepository.save(user);
    }

    // ── Đăng nhập bằng email/mật khẩu ───────────────────────────────────────

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Email hoặc mật khẩu không đúng"));

        if (user.getPasswordHash() == null) {
            throw new BusinessException("Tài khoản này đăng nhập bằng Google. Vui lòng dùng nút 'Đăng nhập với Google'.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("Email hoặc mật khẩu không đúng");
        }

        checkAccountStatus(user);
        return buildAuthResponse(user);
    }

    // ── Đăng nhập bằng Google ────────────────────────────────────────────────

    @Transactional
    public AuthResponse loginWithGoogle(GoogleAuthRequest request) {
        GoogleIdToken.Payload payload = verifyGoogleToken(request.getIdToken());
        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String fullName = (String) payload.get("name");
        String avatar = (String) payload.get("picture");
        return resolveGoogleSignIn(googleId, email, fullName, avatar);
    }

    /**
     * FR-006: auto-link only when local email already verified (ACTIVE);
     * otherwise OTP + complete-link (anti takeover).
     */
    AuthResponse resolveGoogleSignIn(String googleId, String email, String fullName, String avatar) {
        Optional<User> existingByGoogleId = userRepository.findByGoogleId(googleId);
        Optional<User> existingByEmail = userRepository.findByEmail(email);

        if (existingByGoogleId.isPresent()) {
            User user = existingByGoogleId.get();
            checkAccountStatus(user);
            return buildAuthResponse(user);
        }

        if (existingByEmail.isPresent()) {
            User user = existingByEmail.get();
            if (user.getStatus() == User.Status.SUSPENDED) {
                throw new BusinessException("Tài khoản đã bị tạm khóa. Vui lòng liên hệ hỗ trợ.");
            }
            if (user.getStatus() == User.Status.ACTIVE) {
                user.setGoogleId(googleId);
                if (user.getAvatarUrl() == null && avatar != null) {
                    user.setAvatarUrl(avatar);
                }
                userRepository.save(user);
                return buildAuthResponse(user);
            }

            // INACTIVE local account — do NOT auto-link; require OTP (FR-006)
            issueAndSendOtp(user, "Xác minh liên kết Google");
            userRepository.save(user);
            String verificationToken = jwtTokenProvider.generateGoogleLinkToken(
                    googleId, email, fullName, avatar);
            return AuthResponse.pendingGoogleLink(verificationToken, email);
        }

        User user = new User();
        user.setFullName(fullName != null ? fullName : email);
        user.setEmail(email);
        user.setGoogleId(googleId);
        user.setAvatarUrl(avatar);
        user.setRole(User.Role.CUSTOMER);
        user.setStatus(User.Status.ACTIVE);
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse completeGoogleLink(GoogleCompleteLinkRequest request) {
        Claims claims;
        try {
            claims = jwtTokenProvider.parseGoogleLinkToken(request.getVerificationToken());
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException("Phiên liên kết Google không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập Google lại.");
        }

        String email = claims.get("email", String.class);
        String googleId = claims.get("googleId", String.class);
        String fullName = claims.get("fullName", String.class);
        String avatar = claims.get("avatar", String.class);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

        if (user.getStatus() == User.Status.SUSPENDED) {
            throw new BusinessException("Tài khoản đã bị tạm khóa. Vui lòng liên hệ hỗ trợ.");
        }

        validateOtp(user, request.getOtpCode());

        user.setGoogleId(googleId);
        user.setStatus(User.Status.ACTIVE);
        if (user.getAvatarUrl() == null && avatar != null) {
            user.setAvatarUrl(avatar);
        }
        if (fullName != null && (user.getFullName() == null || user.getFullName().isBlank())) {
            user.setFullName(fullName);
        }
        clearOtp(user);
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    // ── Quên/Đặt lại mật khẩu ───────────────────────────────────────────────

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // US5 / FR-008: never reveal whether the email exists (or is Google-only).
        Optional<User> found = userRepository.findByEmail(request.getEmail());
        if (found.isEmpty()) {
            log.info("Forgot-password requested for unknown email (no disclosure)");
            return;
        }

        User user = found.get();
        if (user.getPasswordHash() == null) {
            log.info("Forgot-password skipped for Google-only account {}", user.getEmail());
            return;
        }

        issueAndSendOtp(user, "Đặt lại mật khẩu");
        userRepository.save(user);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // FR-008: never reveal whether the email exists (same generic OTP error as wrong code).
        Optional<User> found = userRepository.findByEmail(request.getEmail());
        if (found.isEmpty()) {
            log.info("Reset-password requested for unknown email (no disclosure)");
            throw new BusinessException("Mã OTP không đúng. Vui lòng kiểm tra lại.");
        }

        User user = found.get();
        if (user.getPasswordHash() == null) {
            log.info("Reset-password rejected for Google-only account {}", user.getEmail());
            throw new BusinessException("Mã OTP không đúng. Vui lòng kiểm tra lại.");
        }

        validateOtp(user, request.getOtpCode());

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        clearOtp(user);
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUser(user);
    }

    // ── Refresh / Logout ─────────────────────────────────────────────────────

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken token = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BusinessException("Refresh token không hợp lệ"));

        if (!token.isValid()) {
            throw new BusinessException("Refresh token đã hết hạn hoặc bị thu hồi");
        }

        User user = token.getUser();
        checkAccountStatus(user);

        token.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(token);

        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByToken(request.getRefreshToken())
                .ifPresent(token -> {
                    token.setRevokedAt(LocalDateTime.now());
                    refreshTokenRepository.save(token);
                });
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private GoogleIdToken.Payload verifyGoogleToken(String idToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                throw new BusinessException("Google token không hợp lệ hoặc đã hết hạn");
            }
            return googleIdToken.getPayload();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Xác thực Google thất bại: " + e.getMessage());
        }
    }

    private void checkAccountStatus(User user) {
        if (user.getStatus() == User.Status.INACTIVE) {
            // Staff: Admin activation only — do not issue OTP (blocks self-activate path).
            if (isStaffRole(user.getRole())) {
                throw new BusinessException(
                        "Tài khoản chưa được kích hoạt. Vui lòng liên hệ Admin.");
            }
            issueAndSendOtp(user, "Xác thực tài khoản");
            userRepository.save(user);
            throw new AccountNotVerifiedException(user.getEmail());
        }
        if (user.getStatus() == User.Status.SUSPENDED) {
            throw new BusinessException("Tài khoản đã bị tạm khóa. Vui lòng liên hệ hỗ trợ.");
        }
    }

    /** Employee / Manager / Admin — not self-service Customer registration. */
    private static boolean isStaffRole(User.Role role) {
        return role == User.Role.EMPLOYEE
                || role == User.Role.MANAGER
                || role == User.Role.ADMIN;
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateToken(user.getId());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(refreshExpirationDays));
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, refreshToken.getToken(),
                AuthResponse.fromUser(user));
    }

    private void issueAndSendOtp(User user, String subject) {
        enforceOtpSendQuota(user);
        String otp = generateOtp();
        user.setOtpCode(otp);
        user.setOtpExpiredAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES));
        sendOtpEmail(user.getEmail(), otp, subject);
    }

    private void enforceOtpSendQuota(User user) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = user.getOtpSendWindowStart();
        if (windowStart == null || windowStart.isBefore(now.minusHours(1))) {
            user.setOtpSendWindowStart(now);
            user.setOtpSendCount(0);
        }
        if (user.getOtpSendCount() >= OTP_MAX_SENDS_PER_HOUR) {
            throw new BusinessException(
                    "Bạn đã gửi quá " + OTP_MAX_SENDS_PER_HOUR
                            + " mã OTP trong 1 giờ. Vui lòng thử lại sau.");
        }
        user.setOtpSendCount(user.getOtpSendCount() + 1);
    }

    private void clearOtp(User user) {
        user.setOtpCode(null);
        user.setOtpExpiredAt(null);
    }

    private void validateOtp(User user, String otpCode) {
        if (user.getOtpCode() == null || !user.getOtpCode().equals(otpCode)) {
            throw new BusinessException("Mã OTP không đúng. Vui lòng kiểm tra lại.");
        }
        if (user.getOtpExpiredAt() == null || LocalDateTime.now().isAfter(user.getOtpExpiredAt())) {
            throw new OtpExpiredException("Mã OTP đã hết hạn. Vui lòng yêu cầu gửi lại mã mới.");
        }
    }

    private String generateOtp() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    private void sendOtpEmail(String to, String otp, String subject) {
        String from = resolveMailFrom();
        if (!StringUtils.hasText(from) || "no-reply@dev.local".equalsIgnoreCase(from.trim())) {
            throw new BusinessException(
                    "Không gửi được email OTP: SMTP chưa cấu hình. "
                            + "Đặt MAIL_USERNAME và MAIL_PASSWORD (Gmail App Password) trong file .env ở thư mục gốc dự án, rồi khởi động lại backend.");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from.trim());
            helper.setTo(to);
            helper.setSubject("[Homestay] " + subject);
            helper.setText(
                    "Xin chào,\n\n"
                            + "Mã xác thực của bạn là: " + otp + "\n"
                            + "Mã có hiệu lực trong " + OTP_TTL_MINUTES + " phút.\n"
                            + "Vui lòng không chia sẻ mã này với ai.\n\n"
                            + "Trân trọng,\nĐội ngũ Homestay",
                    false
            );
            mailSender.send(message);
            log.info("OTP email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", to, e.getMessage());
            throw new BusinessException(
                    "Không gửi được email OTP. Kiểm tra MAIL_USERNAME / MAIL_PASSWORD "
                            + "(Google cần App Password 16 ký tự) và MAIL_HOST nếu không dùng Gmail/Workspace.");
        }
    }

    private String resolveMailFrom() {
        if (StringUtils.hasText(mailFrom) && !"no-reply@dev.local".equalsIgnoreCase(mailFrom.trim())) {
            return mailFrom.trim();
        }
        if (mailSender instanceof org.springframework.mail.javamail.JavaMailSenderImpl impl
                && StringUtils.hasText(impl.getUsername())) {
            return impl.getUsername().trim();
        }
        return mailFrom;
    }
}
