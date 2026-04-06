# Summary & Change Log - JWT Auth Reform

**Status:** DRAFT → IN_PROGRESS (Phase 0 Complete)
**Date:** 2026-04-03
**Module:** `com.sonnk.auth` (port 8084) + downstream services

---

## Section 1: Phân Tích Codebase Hiện Tại

### Auth Service - Tổng Quan

| Component | File | Trạng Thái |
|-----------|------|------------|
| JWT Generation | `JwtTokenService.java` | ✅ Hoạt động (custom HMAC-SHA256, không dùng thư viện ngoài) |
| JWT Validation/Parse | _(không có)_ | ❌ Thiếu - không thể verify token đến |
| Security Config | `SecurityConfig.java` | ⚠️ Minimal - chỉ BCrypt + permit `/api/auth/**` |
| Auth Logic | `AuthService.java` | ⚠️ Demo - hardcoded `admin/admin` |
| UserDetailsService | _(không có)_ | ❌ Thiếu - Spring Security không biết load user |
| JWT Auth Filter | _(không có)_ | ❌ Thiếu - không filter Bearer token ở protected endpoints |
| RefreshToken DB | _(không có)_ | ❌ Thiếu - refresh token không lưu, không thể revoke |
| Session Policy | _(mặc định)_ | ⚠️ Chưa set STATELESS |
| Method Security | _(không có)_ | ❌ Thiếu - không có `@PreAuthorize` |

### Vấn Đề Cốt Lõi

1. **Chỉ generate, không validate:** `JwtTokenService` tạo được token nhưng không có logic verify signature, kiểm tra expiry, hay extract claims từ token đến.
2. **Filter chain trống:** Không có `OncePerRequestFilter` nào chặn request và kiểm tra `Authorization: Bearer` header.
3. **Không có UserDetailsService:** Spring Security không có cơ chế load `UserDetails` từ DB → `AuthenticationManager` không hoạt động.
4. **Hardcoded credentials:** `AuthService.login()` chỉ cho phép `admin/admin` (dòng 32) - không kết nối DB hay user-service.
5. **Refresh token phantom:** Token được sinh ra nhưng không lưu DB → không thể revoke, rotate, hay detect reuse.

### User Service - User Entity

`UserModel` tại `user/src/main/java/com/sonnk/user/model/entity/UserModel.java`:
- Roles: `CUSTOMER, EMPLOYEE, MANAGER, ADMIN`
- Status: `ACTIVE, INACTIVE, LOCKED`
- Có `passwordHash` → BCrypt verify
- Auth service hiện chưa integrate với user-service

---

## Section 2: Spring Security 6 vs Legacy

### Tại Sao Spring Security 6

Spring Boot 3.x bundled Spring Security 6. `WebSecurityConfigurerAdapter` đã bị **remove hoàn toàn** từ SS 6.0 (không chỉ deprecated).

| Feature | Spring Security 5 (Legacy) | Spring Security 6 (Current) |
|---------|---------------------------|------------------------------|
| Config approach | Extend `WebSecurityConfigurerAdapter` | `@Bean SecurityFilterChain` |
| DSL style | Override methods | Lambda DSL (bắt buộc) |
| `authorizeRequests()` | ✅ Available | ❌ Removed → dùng `authorizeHttpRequests()` |
| `antMatchers()` | ✅ Available | ❌ Removed → dùng `requestMatchers()` |
| Method Security annotation | `@EnableGlobalMethodSecurity(prePostEnabled=true)` | `@EnableMethodSecurity` (prePost enabled mặc định) |
| `@PreAuthorize` | Cần bật manually | Bật sẵn khi dùng `@EnableMethodSecurity` |
| CSRF default | Enabled | Enabled (tắt explicit qua lambda) |
| Stateless config | `http.sessionManagement().sessionCreationPolicy(STATELESS)` | `http.sessionManagement(s -> s.sessionCreationPolicy(STATELESS))` |

### SecurityConfig Hiện Tại - Đánh Giá

```java
// auth/src/main/java/com/sonnk/auth/config/SecurityConfig.java
// → ĐÚNG SS6 pattern rồi. Thiếu 3 điều:
// 1. .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
// 2. .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
// 3. @EnableMethodSecurity trên class (hoặc config class riêng)
```

---

## Section 3: Kiến Trúc JWT - Quyết Định Thư Viện

### Tại Sao Chọn JJWT (io.jsonwebtoken 0.12.6)

| Tiêu chí | Custom (hiện tại) | JJWT 0.12.x |
|----------|------------------|-------------|
| Generate | ✅ Hoạt động | ✅ Fluent builder |
| Validate signature | ❌ Không có | ✅ Tự động qua `verifyWith(key)` |
| Expiry check | ❌ Không có | ✅ Tự động throw `ExpiredJwtException` |
| Extract claims | ❌ Không có | ✅ `.getPayload().getSubject()` etc. |
| Exception types | ❌ Không có | ✅ `JwtException`, `ExpiredJwtException`, `MalformedJwtException` |
| Standard compliance | ⚠️ Manual, dễ lỗi | ✅ RFC 7519 compliant |
| Learning value | ✅ Hiểu internals | ✅ Industry standard usage |

### Dependency Cần Thêm (Phase 1 - Sau Approval)

```xml
<!-- auth/pom.xml - THÊM sau khi user approve -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<!-- Spring Data JPA (nếu auth service cần DB cho RefreshToken) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.mariadb.jdbc</groupId>
    <artifactId>mariadb-java-client</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## Section 4: Core Components - Minimal Runnable Examples

### 4.1 JwtService (sandbox.security.service)

```java
@Service
public class JwtService {
    private final SecretKey secretKey;
    private final long accessTtl;
    private final long refreshTtl;

    public JwtService(AuthJwtProperties props) {
        // Key phải >= 256 bits cho HS256
        byte[] keyBytes = props.getSecret().getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTtl = props.getAccessTokenTtlSeconds();
        this.refreshTtl = props.getRefreshTokenTtlSeconds();
    }

    public String generateAccessToken(String subject, List<String> roles) {
        return Jwts.builder()
                .subject(subject)
                .claim("roles", roles)
                .claim("typ", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTtl * 1000))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(String subject) {
        return Jwts.builder()
                .subject(subject)
                .claim("typ", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTtl * 1000))
                .signWith(secretKey)
                .compact();
    }

    // Throws JwtException subtypes on invalid token (auto expiry + signature check)
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

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = validateAndExtract(token);
            return claims.getSubject().equals(userDetails.getUsername());
            // Expiry đã được kiểm tra tự động trong validateAndExtract()
        } catch (JwtException e) {
            return false;
        }
    }
}
```

### 4.2 JwtAuthenticationFilter (sandbox.security.filter)

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        // Không có Bearer → tiếp tục chain (Spring Security sẽ reject nếu cần auth)
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            String username = jwtService.extractUsername(token);
            // Chỉ set auth nếu chưa có trong SecurityContext (avoid overwrite)
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtService.isTokenValid(token, userDetails)) {
                    var auth = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (JwtException e) {
            // Token không hợp lệ → không set auth → Spring Security tự return 401
        }

        filterChain.doFilter(request, response);
    }
}
```

### 4.3 SandboxSecurityConfig (sandbox.security.config)

```java
@Configuration
@EnableMethodSecurity   // Kích hoạt @PreAuthorize, @PostAuthorize
public class SandboxSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain sandboxSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/sandbox/auth/**").permitAll()  // Login/register không cần token
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
```

### 4.4 UserEntity (sandbox.security.entity)

```java
@Entity
@Table(name = "sandbox_users")
public class UserEntity implements UserDetails {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SandboxRole role;  // ADMIN, USER (simplified for sandbox)

    // UserDetails interface - map role → GrantedAuthority với prefix ROLE_
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
```

---

## Section 5: Refresh Token - Rotation & Reuse Detection

### Flow Diagram

```
Client                    Auth Service                  DB
  │                            │                         │
  ├─── POST /refresh ─────────►│                         │
  │    { refreshToken: T1 }    │──── findByTokenHash ───►│
  │                            │◄─── entity (active) ────│
  │                            │                         │
  │                            │──── revoke T1 ─────────►│
  │                            │──── save T2 (new) ──────►│
  │                            │                         │
  │◄─── { accessToken: A2,     │                         │
  │       refreshToken: T2 } ──│                         │
  │                            │                         │

# Reuse Detection (T1 đã bị revoke được dùng lại):
  ├─── POST /refresh ─────────►│
  │    { refreshToken: T1 }    │──── findByTokenHash ───►│
  │                            │◄─── entity (REVOKED) ───│
  │                            │──── REVOKE ALL tokens ──►│  ← Security Alert!
  │◄─── 401 Unauthorized ──────│
```

### RefreshTokenEntity

```java
@Entity
@Table(name = "sandbox_refresh_tokens")
public class RefreshTokenEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String tokenHash;    // SHA-256 hash của raw token

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
```

---

## Section 6: Metrics & Impact Analysis

| Metric | Trước | Sau |
|--------|-------|-----|
| JWT validation | Không có | Signature + expiry tự động (JJWT) |
| Xác thực user | `admin/admin` hardcoded | BCrypt verify từ DB |
| Bảo vệ endpoint | Chỉ `requestMatchers` | `requestMatchers` + `@PreAuthorize` |
| Session state | Mặc định (stateful) | `STATELESS` |
| Refresh token | Generated, không track | DB-stored, revocable, rotation |
| Token rotation | Không có | Mỗi lần refresh |
| Reuse detection | Không có | Revoke all khi detect |
| `@EnableMethodSecurity` | Không có | Bật, `@PreAuthorize` hoạt động |
| UserDetailsService | Không có | DB-backed (`UserSandboxRepository`) |

---

## Section 7: Testing Strategy

### Unit Tests (JwtServiceTest)

```java
class JwtServiceTest {

    @Test void generateAccessToken_validInput_returnsThreeParts() {
        // "eyJ...".split("\\.").length == 3
    }

    @Test void validateAndExtract_validToken_returnsCorrectSubject() {
        // claims.getSubject() == "testuser"
    }

    @Test void validateAndExtract_expiredToken_throwsExpiredJwtException() {
        // Tạo token với TTL = -1 second → parse → expect ExpiredJwtException
    }

    @Test void validateAndExtract_tamperedToken_throwsSignatureException() {
        // Thay đổi ký tự cuối signature → expect SignatureException
    }

    @Test void isTokenValid_validTokenMatchingUser_returnsTrue() { }
    @Test void isTokenValid_expiredToken_returnsFalse() { }
    @Test void isTokenValid_wrongSubject_returnsFalse() { }
}
```

### RBAC Integration Test (RbacIntegrationTest)

```java
@SpringBootTest
@AutoConfigureMockMvc
class RbacIntegrationTest {

    @Test
    void userToken_accessAdminEndpoint_returns403() throws Exception {
        // 1. Tạo user với role USER
        // 2. Login → lấy accessToken
        // 3. GET /api/admin/dashboard với USER token
        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().isForbidden());  // 403
    }

    @Test
    void adminToken_accessAdminEndpoint_returns200() throws Exception {
        // 1. Login với ADMIN credentials
        // 2. GET /api/admin/dashboard với ADMIN token
        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().isOk());  // 200
    }

    @Test
    void noToken_accessProtectedEndpoint_returns401() throws Exception {
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isUnauthorized());  // 401
    }
}
```

---

## Section 8: Quickstart Commands

```bash
# Chạy auth service
cd d:\1_Learning\Project\PetBoby\auth
mvn spring-boot:run

# Test login hiện tại (demo - hardcoded admin/admin)
curl -X POST http://localhost:8084/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'

# Chạy toàn bộ tests của auth module
mvn test -pl auth

# Chạy test cụ thể
mvn test -pl auth -Dtest=JwtServiceTest
mvn test -pl auth -Dtest=RbacIntegrationTest

# Verify JWT token (sau khi login - decode payload base64)
# Linux/Mac:
TOKEN="eyJ..."
echo $TOKEN | cut -d. -f2 | base64 -d

# Build auth module
mvn package -pl auth -DskipTests
```

---

## Change Log

| Ngày | Phase | Thay Đổi | Tác Giả |
|------|-------|----------|---------|
| 2026-04-03 | Phase 0 | Phân tích codebase + tạo Implementation Plan (DRAFT) + Summary | Claude Code |
| 2026-04-03 | Phase 1 | Sandbox: JJWT+JPA+H2, UserEntity, RefreshTokenEntity, JwtService, UserDetailsServiceImpl, RefreshTokenService, JwtAuthenticationFilter, SandboxSecurityConfig, DTOs, SandboxAuthController (17 files) | Claude Code |
| 2026-04-03 | Phase 2 | Tests: JwtServiceTest (9), JwtAuthFilterTest (4), RbacIntegrationTest (5) = 18 test methods | Claude Code |
| _(pending)_ | Phase 3 | Production migration: SecurityConfig, AuthService, JwtTokenService → JwtService | _(awaiting Phase 3 Proceed)_ |

---

*Last Updated: 2026-04-03 - Phase 0 Complete. Chờ lệnh "Proceed Phase 1"*
