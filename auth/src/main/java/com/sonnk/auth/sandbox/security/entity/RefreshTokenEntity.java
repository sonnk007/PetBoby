package com.sonnk.auth.sandbox.security.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Lưu trữ refresh token trong DB để hỗ trợ:
 * - Revocation: đánh dấu token đã dùng/thu hồi.
 * - Rotation: mỗi lần refresh → token cũ bị revoke, token mới được cấp.
 * - Reuse Detection: nếu token đã revoked được dùng lại → thu hồi toàn bộ session của user.
 *
 * Không lưu raw token. Chỉ lưu SHA-256 hash để so sánh an toàn.
 */
@Entity
@Table(name = "sandbox_refresh_tokens")
public class RefreshTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // SHA-256 hash của raw refresh token JWT
    @Column(nullable = false, unique = true, length = 512)
    private String tokenHash;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Instant expiresAt;

    // true nếu đã bị revoke (đã dùng để rotation hoặc bị thu hồi thủ công)
    @Column(nullable = false)
    private boolean revoked = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    // ---- Getters/Setters ----

    public Long getId() { return id; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
