package com.sonnk.auth.sandbox.security.service;

import com.sonnk.auth.config.AuthJwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * JWT service dùng JJWT 0.12.x.
 *
 * So sánh với JwtTokenService (production):
 * - JwtTokenService: chỉ generate, không validate (custom HMAC-SHA256 thủ công).
 * - JwtService (này): generate + parse + validate đầy đủ (JJWT library).
 *
 * JJWT tự động kiểm tra:
 * - Signature (ký sai → SignatureException).
 * - Expiry (hết hạn → ExpiredJwtException).
 * - Cấu trúc token (sai format → MalformedJwtException).
 *
 * Tất cả exception trên là subclass của JwtException → bắt một chỗ là đủ.
 */
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;

    public JwtService(AuthJwtProperties props) {
        // Keys.hmacShaKeyFor ném WeakKeyException nếu secret < 32 bytes (256 bits)
        byte[] keyBytes = props.getSecret().getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTtlSeconds = props.getAccessTokenTtlSeconds();
        this.refreshTtlSeconds = props.getRefreshTokenTtlSeconds();
    }

    /**
     * Tạo access token với subject (username) và danh sách roles.
     * typ=access để phân biệt với refresh token.
     */
    public String generateAccessToken(String subject, List<String> roles) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claim("roles", roles)
                .claim("typ", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTtlSeconds * 1000))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Tạo refresh token. Payload tối giản (chỉ sub + typ + exp) vì chỉ dùng để xác định user khi rotate.
     */
    /**
     * jti (JWT ID) = UUID ngẫu nhiên đảm bảo mỗi refresh token là duy nhất
     * ngay cả khi được sinh trong cùng một millisecond cho cùng một user.
     * Điều này tránh lỗi unique constraint khi hash được lưu vào DB.
     */
    public String generateRefreshToken(String subject) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claim("typ", "refresh")
                .id(UUID.randomUUID().toString())   // jti: đảm bảo token luôn unique
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTtlSeconds * 1000))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Parse và validate token. Ném JwtException (và các subclass) nếu không hợp lệ.
     * JJWT tự động verify signature và expiry.
     */
    public Claims validateAndExtract(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return validateAndExtract(token).getSubject();
    }

    /**
     * Kiểm tra token hợp lệ và khớp với userDetails.
     * Return false thay vì ném exception → dùng trong filter (không nên throw từ filter).
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = validateAndExtract(token);
            return claims.getSubject().equals(userDetails.getUsername());
            // Expiry đã được validateAndExtract() kiểm tra tự động.
        } catch (JwtException e) {
            return false;
        }
    }

    public long getAccessTtlSeconds() {
        return accessTtlSeconds;
    }

    public long getRefreshTtlSeconds() {
        return refreshTtlSeconds;
    }
}
