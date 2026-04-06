package com.sonnk.auth.sandbox.security.api;

import com.sonnk.auth.sandbox.security.api.dto.RefreshRequest;
import com.sonnk.auth.sandbox.security.api.dto.RegisterRequest;
import com.sonnk.auth.sandbox.security.api.dto.SandboxLoginRequest;
import com.sonnk.auth.sandbox.security.api.dto.SandboxLoginResponse;
import com.sonnk.auth.sandbox.security.entity.UserEntity;
import com.sonnk.auth.sandbox.security.repository.UserSandboxRepository;
import com.sonnk.auth.sandbox.security.service.JwtService;
import com.sonnk.auth.sandbox.security.service.RefreshTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Controller sandbox cho JWT Auth flow.
 *
 * Endpoints (tất cả dưới /sandbox/ để SandboxSecurityConfig xử lý):
 *
 * PUBLIC (permitAll):
 *   POST /sandbox/auth/register  - Đăng ký user mới
 *   POST /sandbox/auth/login     - Đăng nhập, nhận access + refresh token
 *   POST /sandbox/auth/refresh   - Rotate refresh token, nhận token mới
 *
 * PROTECTED (authenticated):
 *   GET  /sandbox/protected      - Bất kỳ user đã xác thực (exercise: 401 nếu không có token)
 *
 * ADMIN ONLY (hasRole('ADMIN')):
 *   GET  /sandbox/admin/test     - Exercise RBAC: USER token → 403, ADMIN token → 200
 */
@RestController
@RequestMapping("/sandbox")
public class SandboxAuthController {

    private final AuthenticationManager authenticationManager;
    private final UserSandboxRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    public SandboxAuthController(AuthenticationManager authenticationManager,
                                  UserSandboxRepository userRepository,
                                  JwtService jwtService,
                                  RefreshTokenService refreshTokenService,
                                  PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Đăng ký user mới.
     * Whitelist mapping: chỉ lấy username, password, role từ request - không map toàn bộ vào entity.
     */
    @PostMapping("/auth/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Username already exists: " + request.username());
        }

        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("User registered: " + request.username() + " [" + request.role() + "]");
    }

    /**
     * Đăng nhập: xác thực qua AuthenticationManager → DaoAuthenticationProvider → BCrypt verify.
     * Trả về access token + refresh token.
     */
    @PostMapping("/auth/login")
    public ResponseEntity<SandboxLoginResponse> login(@Valid @RequestBody SandboxLoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserEntity user = (UserEntity) auth.getPrincipal();
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String accessToken = jwtService.generateAccessToken(user.getUsername(), roles);
        String refreshToken = refreshTokenService.issue(user);

        return ResponseEntity.ok(new SandboxLoginResponse(
                accessToken, refreshToken, "Bearer", jwtService.getAccessTtlSeconds()));
    }

    /**
     * Rotate refresh token: revoke token cũ, cấp access token + refresh token mới.
     * Reuse detection: nếu token đã bị revoke được gửi lại → thu hồi toàn bộ session.
     */
    @PostMapping("/auth/refresh")
    public ResponseEntity<SandboxLoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshTokenService.RotationResult result;
        try {
            result = refreshTokenService.rotate(request.refreshToken());
        } catch (SecurityException e) {
            // Reuse detected - all sessions revoked
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }

        return ResponseEntity.ok(new SandboxLoginResponse(
                result.accessToken(), result.refreshToken(), "Bearer",
                jwtService.getAccessTtlSeconds()));
    }

    /**
     * Protected endpoint: bất kỳ user đã xác thực đều truy cập được.
     * Exercise: gọi không có token → 401 Unauthorized.
     */
    @GetMapping("/protected")
    public ResponseEntity<String> protectedEndpoint(@AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok("Hello, " + user.getUsername() + "! You are authenticated.");
    }

    /**
     * ADMIN-only endpoint cho RBAC exercise.
     * Exercise:
     *   - Gọi với USER token → 403 Forbidden  ← Spring Security từ chối @PreAuthorize
     *   - Gọi với ADMIN token → 200 OK
     *   - Gọi không có token → 401 Unauthorized
     */
    @GetMapping("/admin/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> adminTest(@AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok("Admin access granted! Welcome, " + user.getUsername());
    }
}
