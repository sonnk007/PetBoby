package com.sonnk.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service cho use case Auth (login/logout, phát hành JWT).
 *
 * Lưu ý:
 * - Đây là bản **demo** tập trung vào luồng JWT, chưa kết nối thật với user-service.
 * - Tạm thời dùng một user giả (hard-code) để minh hoạ:
 *   username = \"admin\", password = \"admin\".
 *
 * Khi phát triển tiếp:
 * - Thay thế bằng call HTTP sang user-service để verify credential + lấy role thực tế.
 */
@Service
public class AuthService {

    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(JwtTokenService jwtTokenService, PasswordEncoder passwordEncoder) {
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthTokens login(String username, String password) {
        // DEMO ONLY: người dùng giả lập (admin/admin).
        if (!"admin".equals(username) || !"admin".equals(password)) {
            throw new IllegalArgumentException("Invalid username or password (demo auth)");
        }

        // Trong thực tế: load user từ user-service, so sánh password hash (BCrypt).
        // Ở đây tạm hard-code role ADMIN cho user admin.
        String accessToken = jwtTokenService.generateAccessToken(username, List.of("ROLE_ADMIN"));
        String refreshToken = jwtTokenService.generateRefreshToken(username, List.of("ROLE_ADMIN"));
        return new AuthTokens(
                accessToken,
                jwtTokenService.getAccessTokenTtlSeconds(),
                refreshToken,
                jwtTokenService.getRefreshTokenTtlSeconds()
        );
    }

    public void logout(String token) {
        // Với JWT stateless: logout thường được xử lý bằng blacklist/refresh token.
        // Ở đây chỉ là demo, không lưu state logout.
    }

    /**
     * Kết quả login nội bộ (không expose ra ngoài service khác ngoài layer API).
     */
    public record AuthTokens(
            String accessToken,
            long accessTokenExpiresInSeconds,
            String refreshToken,
            long refreshTokenExpiresInSeconds
    ) {
    }
}

