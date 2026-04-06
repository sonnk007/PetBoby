package com.sonnk.auth.sandbox.security.service;

import com.sonnk.auth.sandbox.security.entity.RefreshTokenEntity;
import com.sonnk.auth.sandbox.security.entity.UserEntity;
import com.sonnk.auth.sandbox.security.repository.RefreshTokenRepository;
import com.sonnk.auth.sandbox.security.repository.UserSandboxRepository;
import io.jsonwebtoken.JwtException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

/**
 * Quản lý vòng đời refresh token.
 *
 * Rotation flow:
 *   Client gửi refreshToken T1
 *   → T1 bị revoke
 *   → T2 được cấp
 *   → Client nhận T2 + access token mới
 *
 * Reuse detection:
 *   Nếu T1 (đã revoked) được gửi lại
 *   → Toàn bộ refresh tokens của user bị revoke
 *   → Client bị đăng xuất khỏi tất cả thiết bị
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserSandboxRepository userRepository;
    private final JwtService jwtService;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                                UserSandboxRepository userRepository,
                                JwtService jwtService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    /**
     * Cấp refresh token mới cho user và lưu hash vào DB.
     *
     * @return raw refresh token JWT (gửi về client, không lưu)
     */
    @Transactional
    public String issue(UserEntity user) {
        String rawToken = jwtService.generateRefreshToken(user.getUsername());
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setTokenHash(hashToken(rawToken));
        entity.setUserId(user.getId());
        entity.setExpiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()));
        entity.setRevoked(false);
        entity.setCreatedAt(Instant.now());
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    /**
     * Rotate refresh token: revoke T1, cấp T2.
     *
     * @return RotationResult chứa access token mới + refresh token mới
     * @throws SecurityException nếu phát hiện reuse (token đã bị revoke được dùng lại)
     * @throws IllegalArgumentException nếu token không tồn tại hoặc đã hết hạn
     */
    @Transactional
    public RotationResult rotate(String rawRefreshToken) {
        // Validate JWT trước (signature + expiry) - ném JwtException nếu sai
        String username;
        try {
            username = jwtService.extractUsername(rawRefreshToken);
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid refresh token: " + e.getMessage(), e);
        }

        String hash = hashToken(rawRefreshToken);
        RefreshTokenEntity existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found in DB"));

        if (existing.isRevoked()) {
            // Reuse detection: thu hồi toàn bộ session của user
            long ownerId = existing.getUserId();
            refreshTokenRepository.revokeAllActiveByUserId(ownerId);
            throw new SecurityException(
                    "Refresh token reuse detected for user [" + username + "]. All sessions revoked.");
        }

        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token expired");
        }

        // Revoke token cũ
        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        // Cấp token mới
        UserEntity user = userRepository.findById(existing.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String newAccessToken = jwtService.generateAccessToken(username, roles);
        String newRefreshToken = issue(user);

        return new RotationResult(newAccessToken, newRefreshToken);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Kết quả rotation: access token mới + refresh token mới.
     */
    public record RotationResult(String accessToken, String refreshToken) {}
}
