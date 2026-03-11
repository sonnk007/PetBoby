package com.sonnk.auth.api;

import com.sonnk.auth.api.dto.LoginRequest;
import com.sonnk.auth.api.dto.LoginResponse;
import com.sonnk.auth.config.AuthJwtProperties;
import com.sonnk.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API cho auth service.
 *
 * Nguyên tắc (theo Clean API rules):
 * - Dùng DTO để nhận request/ trả response.
 * - Validate input với @Valid + Bean Validation.
 * - Gọi AuthService thực thi use case, không chứa logic nghiệp vụ.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthJwtProperties jwtProperties;

    public AuthController(AuthService authService, AuthJwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    /**
     * Login demo:
     * - Body: { \"username\": \"admin\", \"password\": \"admin\" }
     * - Trả về JWT access token + refresh token kiểu Bearer.
     *
     * Lưu ý:
     * - Chỉ là DEMO để học JWT; không dùng password/secret hard-code trong production.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var tokens = authService.login(request.username(), request.password());
        LoginResponse response = new LoginResponse(
                tokens.accessToken(),
                "Bearer",
                tokens.accessTokenExpiresInSeconds(),
                tokens.refreshToken(),
                tokens.refreshTokenExpiresInSeconds()
        );
        return ResponseEntity.ok(response);
    }

    // Logout demo: với JWT stateless, thường xử lý bằng blacklist/refresh token.
    // Ở đây để đơn giản, chưa triển khai endpoint logout riêng.
}

