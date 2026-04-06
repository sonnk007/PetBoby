package com.sonnk.auth.sandbox.security;

import com.sonnk.auth.config.AuthJwtProperties;
import com.sonnk.auth.sandbox.security.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test cho JwtService.
 *
 * Không cần Spring context (@SpringBootTest) vì JwtService chỉ phụ thuộc vào AuthJwtProperties.
 * Dùng fake AuthJwtProperties để kiểm soát TTL (đặc biệt khi test expired token).
 */
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        AuthJwtProperties props = new AuthJwtProperties();
        props.setSecret("test-secret-key-for-unit-testing!!");  // 36 chars >= 32
        props.setIssuer("test-issuer");
        props.setAccessTokenTtlSeconds(3600);
        props.setRefreshTokenTtlSeconds(86400);
        jwtService = new JwtService(props);
    }

    // ---- Access Token ----

    @Test
    void generateAccessToken_validInput_returnsThreePartJwt() {
        String token = jwtService.generateAccessToken("alice", List.of("ROLE_USER"));

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);  // header.payload.signature
    }

    @Test
    void validateAndExtract_validAccessToken_returnsCorrectSubject() {
        String token = jwtService.generateAccessToken("alice", List.of("ROLE_USER"));

        Claims claims = jwtService.validateAndExtract(token);

        assertThat(claims.getSubject()).isEqualTo("alice");
    }

    @Test
    void validateAndExtract_validAccessToken_containsRoles() {
        String token = jwtService.generateAccessToken("bob", List.of("ROLE_ADMIN"));

        Claims claims = jwtService.validateAndExtract(token);

        assertThat(claims.get("roles", List.class)).contains("ROLE_ADMIN");
    }

    @Test
    void validateAndExtract_expiredToken_throwsExpiredJwtException() {
        // Tạo service với TTL = -1 giây (token sinh ra đã hết hạn ngay lập tức)
        AuthJwtProperties shortProps = new AuthJwtProperties();
        shortProps.setSecret("test-secret-key-for-unit-testing!!");
        shortProps.setAccessTokenTtlSeconds(-1);
        shortProps.setRefreshTokenTtlSeconds(-1);
        JwtService shortLivedService = new JwtService(shortProps);

        String expiredToken = shortLivedService.generateAccessToken("alice", List.of("ROLE_USER"));

        assertThatThrownBy(() -> jwtService.validateAndExtract(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void validateAndExtract_tamperedSignature_throwsJwtException() {
        String token = jwtService.generateAccessToken("alice", List.of("ROLE_USER"));

        // Thay đổi ký tự cuối của signature → signature không còn hợp lệ
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        assertThatThrownBy(() -> jwtService.validateAndExtract(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void validateAndExtract_wrongSecret_throwsJwtException() {
        // Token ký với service khác (secret khác) không hợp lệ với service này
        AuthJwtProperties otherProps = new AuthJwtProperties();
        otherProps.setSecret("completely-different-secret-key!!");
        JwtService otherService = new JwtService(otherProps);

        String tokenFromOther = otherService.generateAccessToken("alice", List.of("ROLE_USER"));

        assertThatThrownBy(() -> jwtService.validateAndExtract(tokenFromOther))
                .isInstanceOf(JwtException.class);
    }

    // ---- Refresh Token ----

    @Test
    void generateRefreshToken_validInput_containsRefreshType() {
        String token = jwtService.generateRefreshToken("alice");

        Claims claims = jwtService.validateAndExtract(token);

        assertThat(claims.get("typ", String.class)).isEqualTo("refresh");
    }

    // ---- isTokenValid ----

    @Test
    void isTokenValid_validTokenMatchingUser_returnsTrue() {
        String token = jwtService.generateAccessToken("alice", List.of("ROLE_USER"));
        UserDetails userDetails = User.withUsername("alice")
                .password("irrelevant").roles("USER").build();

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_validTokenWrongUsername_returnsFalse() {
        String token = jwtService.generateAccessToken("alice", List.of("ROLE_USER"));
        UserDetails differentUser = User.withUsername("bob")
                .password("irrelevant").roles("USER").build();

        assertThat(jwtService.isTokenValid(token, differentUser)).isFalse();
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        AuthJwtProperties shortProps = new AuthJwtProperties();
        shortProps.setSecret("test-secret-key-for-unit-testing!!");
        shortProps.setAccessTokenTtlSeconds(-1);
        JwtService shortLivedService = new JwtService(shortProps);

        String expiredToken = shortLivedService.generateAccessToken("alice", List.of("ROLE_USER"));
        UserDetails userDetails = User.withUsername("alice")
                .password("irrelevant").roles("USER").build();

        // isTokenValid phải trả false, không throw exception
        assertThat(jwtService.isTokenValid(expiredToken, userDetails)).isFalse();
    }
}
