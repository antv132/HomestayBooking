package com.homestay.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // cho phép dùng @PreAuthorize trong service/controller
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          JwtAuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Tắt CSRF vì dùng JWT stateless
            .csrf(AbstractHttpConfigurer::disable)

            // Bật CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Không dùng session
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))


            .authorizeHttpRequests(auth -> auth
                // Auth endpoints - ai cũng gọi được
                .requestMatchers(HttpMethod.POST,
                    "/api/auth/register",
                    "/api/auth/verify-otp",
                    "/api/auth/resend-otp",
                    "/api/auth/login",
                    "/api/auth/google",
                    "/api/auth/refresh",
                    "/api/auth/forgot-password",
                    "/api/auth/reset-password",
                    "/api/v1/auth/**"
                ).permitAll()

                // SCR-01 landing, SCR-07 search, SCR-08 room detail — public read (api-spec v1)
                .requestMatchers(HttpMethod.GET,
                    "/api/v1/public/promotions",
                    "/api/v1/public/promotions/**",
                    "/api/v1/public/stats",
                    "/api/v1/public/search-suggestions",
                    "/api/v1/promotions/active",
                    "/api/v1/properties/featured",
                    "/api/v1/rooms/**"
                ).permitAll()

                // FR-18 Admin promotions — JWT + @PreAuthorize ADMIN
                .requestMatchers("/api/v1/admin/promotions", "/api/v1/admin/promotions/**")
                    .authenticated()

                // FR-06 Admin / Manager property APIs
                .requestMatchers("/api/v1/admin/properties", "/api/v1/admin/properties/**")
                    .authenticated()
                .requestMatchers("/api/v1/manager/properties", "/api/v1/manager/properties/**")
                    .authenticated()

                // Xem danh sách phòng và chi tiết - không cần đăng nhập
                .requestMatchers(HttpMethod.GET,
                    "/api/rooms",
                    "/api/rooms/**",
                    "/api/properties",
                    "/api/properties/**",
                    "/api/public/**",
                    "/api/payments/vnpay/return"
                ).permitAll()

                // Swagger docs (dev only)
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()

                .requestMatchers("/error").permitAll()

                // FR-15 WebSocket / SockJS
                .requestMatchers("/ws", "/ws/**").permitAll()

                // FR-10: contract PDFs only via authenticated API — not public static
                .requestMatchers("/uploads/contracts/**").denyAll()

                // Cho phép truy cập file upload (ảnh) không cần đăng nhập
                .requestMatchers("/uploads/**").permitAll()

                // Tất cả còn lại phải đăng nhập
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
            );

        // Thêm JWT filter trước filter mặc định của Spring
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Mã hóa mật khẩu bằng BCrypt
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // Cho phép frontend (React dev server) gọi API
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Dùng pattern để chấp nhận mọi port localhost khi dev (3000, 3001, 5173, ...)
        // localhost = dev; VPS IP = production demo (same host :80 FE → :8080 API is cross-origin)
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "http://36.50.135.79",
            "http://36.50.135.79:*"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
