# JWT Auth Reform - Implementation Plan

**Status:** IN_PROGRESS (Phase 1 + 2 DONE)
**Date:** 2026-04-03
**Project:** PetBoby Microservices - auth service
**Module:** `com.sonnk.auth` (Spring Boot 3.5.3, port 8084)

---

## Part 1: TODO List (Đồng bộ với TodoWrite)

### Phase 0: Planning (DONE)
- [x] Phân tích codebase hiện tại (auth, user service)
- [x] Tạo Implementation_Plan_JWT_Auth.md (DRAFT)
- [x] Tạo Summary_Change_Log_JWT_Auth.md

### Phase 1: Sandbox Implementation ✅ DONE
- [x] 1.1 Thêm JJWT + JPA + H2 dependency vào `auth/pom.xml` + fix secret ≥ 32 bytes
- [x] 1.2 Tạo `UserEntity` implements `UserDetails` + `SandboxRole` enum trong `sandbox.security.entity`
- [x] 1.3 Tạo `RefreshTokenEntity` lưu DB (tokenHash, userId, expiresAt, revoked)
- [x] 1.4 Tạo `UserSandboxRepository` + `RefreshTokenRepository` (`sandbox.security.repository`)
- [x] 1.5 Tạo `JwtService` với generate + parse + validate sử dụng JJWT (`sandbox.security.service`)
- [x] 1.6 Tạo `UserDetailsServiceImpl` implements `UserDetailsService` (`sandbox.security.service`)
- [x] 1.7 Tạo `RefreshTokenService` với rotation + reuse detection (`sandbox.security.service`)
- [x] 1.8 Tạo `JwtAuthenticationFilter` extends `OncePerRequestFilter` (không dùng @Component)
- [x] 1.9 Tạo `SandboxSecurityConfig` - `@Order(1)`, `securityMatcher("/sandbox/**")`, STATELESS, `@EnableMethodSecurity`
- [x] 1.10 Tạo DTOs: `RegisterRequest`, `SandboxLoginRequest`, `SandboxLoginResponse`, `RefreshRequest`
- [x] 1.11 Tạo `SandboxAuthController` - `/register`, `/login`, `/refresh`, `/protected`, `/admin/test`

### Phase 2: Unit & Integration Tests ✅ DONE
- [x] 2.1 `JwtServiceTest` - 9 test methods (generate, validate, expired, tampered, wrong secret, isTokenValid)
- [x] 2.2 `JwtAuthFilterTest` - 4 test methods (valid Bearer, missing header, invalid, tampered)
- [x] 2.3 `RbacIntegrationTest` - 5 test methods (USER→403, ADMIN→200, noToken→401 ×2, USER→protected→200)

### Phase 3: Production Changes (Skip)
- [ ] 3.1 Migrate `JwtService` thay `JwtTokenService` trong production
- [ ] 3.2 Tích hợp `JwtAuthenticationFilter` + `UserDetailsService` vào `SecurityConfig.java` production
- [ ] 3.3 Thay hardcoded auth bằng DB lookup + BCrypt verify trong `AuthService.java`
- [ ] 3.4 Triển khai `RefreshToken` rotation + reuse detection
- [ ] 3.5 Thêm `@PreAuthorize` vào production controllers
- [ ] 3.6 Liquibase migration: bảng `users`, `refresh_tokens` trong auth-service DB (nếu auth service có DB riêng)

---

## Part 2: Quyết Định Thiết Kế Cấp Cao

### D1: JWT Library - Migrate từ Custom sang JJWT
- **Hiện tại:** `JwtTokenService` tự build JWT bằng `javax.crypto.Mac` (HMAC-SHA256) - **chỉ generate, không validate**
- **Đề xuất:** Thêm **JJWT 0.12.6** (`io.jsonwebtoken`) để có generate + parse + validate chuẩn
- **Lý do:** JJWT cung cấp `JwtParser` tự động kiểm tra signature, expiry, và ném exception rõ ràng
- **Thay thế đã xem xét:** Nimbus JOSE JWT (dùng bởi spring-security-oauth2-resource-server) - phức tạp hơn cần thiết cho learning scope

### D2: Session Policy - STATELESS
- `SessionCreationPolicy.STATELESS` → không lưu server-side session
- Mọi request phải mang `Authorization: Bearer <token>` hợp lệ

### D3: User Entity trong Auth Service
- **Vấn đề:** Auth service hiện không có DB; `UserModel` nằm ở `user-service`
- **Sandbox approach:** `UserEntity` trong `sandbox.security.entity` với embedded **H2** (test) hoặc MariaDB riêng
- **Production approach:** Auth service gọi HTTP sang user-service để verify credential (inter-service call)

### D4: RefreshToken Storage & Rotation
- Lưu **hash** của refresh token (không lưu raw) để phát hiện reuse
- **Rotation:** Mỗi lần `POST /refresh` → issue token mới + revoke token cũ
- **Reuse Detection:** Token đã revoked được dùng lại → revoke **toàn bộ** refresh tokens của user (Security alert)

### D5: RBAC với @EnableMethodSecurity
- `@EnableMethodSecurity(prePostEnabled = true)` trên `@Configuration` class
- `UserRole` enum: `CUSTOMER, EMPLOYEE, MANAGER, ADMIN` → map sang Spring `GrantedAuthority` với prefix `ROLE_`
- `@PreAuthorize("hasRole('ADMIN')")` tại service layer (không chỉ controller)

### D6: Spring Security 6 DSL (thay WebSecurityConfigurerAdapter)
- `SecurityConfig` hiện tại đã dùng đúng SS6 DSL (lambda-based) - **không cần refactor pattern**
- Cần bổ sung: `.sessionManagement(...)`, `.addFilterBefore(jwtFilter, ...)`, `@EnableMethodSecurity`

---

## Part 3: Danh Sách Files Tạo/Sửa

### Sandbox Files (Phase 1 - Không ảnh hưởng production)

```
auth/src/main/java/com/sonnk/auth/sandbox/security/
├── entity/
│   ├── UserEntity.java              [NEW] implements UserDetails - id, username, passwordHash, role
│   └── RefreshTokenEntity.java      [NEW] - tokenHash, userId, expiresAt, revoked, createdAt
├── repository/
│   ├── UserSandboxRepository.java   [NEW] JpaRepository<UserEntity, Long>
│   └── RefreshTokenRepository.java  [NEW] JpaRepository<RefreshTokenEntity, Long>
├── service/
│   ├── JwtService.java              [NEW] generate + parse + validate (JJWT 0.12.x)
│   ├── UserDetailsServiceImpl.java  [NEW] implements UserDetailsService (load from UserSandboxRepository)
│   └── RefreshTokenService.java     [NEW] issue, rotate, revoke, reuse-detect
├── filter/
│   └── JwtAuthenticationFilter.java [NEW] extends OncePerRequestFilter
├── config/
│   └── SandboxSecurityConfig.java   [NEW] SecurityFilterChain + SessionStateless + @EnableMethodSecurity
└── api/
    └── SandboxAuthController.java   [NEW] POST /sandbox/auth/register, /login, /refresh
```

### Test Files (Phase 2)

```
auth/src/test/java/com/sonnk/auth/sandbox/security/
├── JwtServiceTest.java              [NEW] Unit test: generate, validate, expired, tampered
├── JwtAuthFilterTest.java           [NEW] MockMvc filter test
└── RbacIntegrationTest.java         [NEW] @SpringBootTest: 403 exercise
```

### Production Files (⛔ Phase 3 - Cần Approval Riêng)

```
auth/pom.xml                                              [MODIFY] thêm JJWT + Spring Data JPA nếu cần
auth/src/main/java/com/sonnk/auth/
├── config/SecurityConfig.java                            [MODIFY] addFilterBefore + SessionStateless
├── service/AuthService.java                              [MODIFY] DB lookup thay hardcoded
└── service/JwtTokenService.java                          [REPLACE → JwtService.java]
```

---

## Part 4: Exercise Plan (Test Scenarios)

### Scenario A: Đăng ký + Đăng nhập thành công
```
POST /sandbox/auth/register
Body: { "username": "alice", "password": "secret123", "role": "USER" }
→ 201 Created

POST /sandbox/auth/login
Body: { "username": "alice", "password": "secret123" }
→ 200 OK: { "accessToken": "eyJ...", "refreshToken": "eyJ...", "expiresIn": 3600 }
```

### Scenario B: ⛔ RBAC - USER Token → ADMIN Endpoint → 403 Forbidden
```
GET /api/admin/dashboard
Authorization: Bearer <alice-access-token (ROLE_USER)>
→ 403 Forbidden  ← Expected

GET /api/admin/dashboard
Authorization: Bearer <admin-access-token (ROLE_ADMIN)>
→ 200 OK          ← Expected
```

### Scenario C: Refresh Token Rotation
```
POST /sandbox/auth/refresh
Body: { "refreshToken": "eyJ...<valid>" }
→ 200 OK: { "accessToken": "eyJ...<NEW>", "refreshToken": "eyJ...<NEW>" }

# Dùng lại refresh token cũ (đã bị revoke):
POST /sandbox/auth/refresh
Body: { "refreshToken": "eyJ...<OLD>" }
→ 401 Unauthorized: "Refresh token reuse detected - all sessions revoked"
```

### Scenario D: Token Validation Errors
```
# Expired token:
GET /api/protected
Authorization: Bearer <expired-token>
→ 401 Unauthorized

# Tampered token:
GET /api/protected
Authorization: Bearer <modified-signature>
→ 401 Unauthorized

# Missing token:
GET /api/protected
→ 401 Unauthorized
```

---

## Part 5: ⛔ STOP POINT - Approval Required

**Dừng tại đây.** Các thay đổi sau **bắt buộc** chờ approval rõ ràng từ user:

| Thay đổi | Rủi ro | Cần Approval |
|----------|--------|--------------|
| Sửa `auth/pom.xml` (thêm JJWT) | Build fail nếu version conflict | ✅ YES |
| Sửa `SecurityConfig.java` production | Có thể block toàn bộ auth flow | ✅ YES |
| Sửa `AuthService.java` production | Breaking change login flow | ✅ YES |
| Thêm DB vào auth-service | Schema migration, startup failure | ✅ YES |
| Replace `JwtTokenService` | Existing tokens invalidated | ✅ YES |
| Thêm `@PreAuthorize` vào controllers | Có thể 403 các endpoint đang hoạt động | ✅ YES |

**Approval Gate Sequence:**
1. **"Proceed Phase 1"** → Bắt đầu sandbox implementation (Phase 1 + 2)
2. **"Proceed Phase 3"** → Bắt đầu production migration (Phase 3) - sau khi Phase 1+2 hoàn chỉnh

---

## Part 6: Deliverables Checklist

### Phase 1 Deliverables
- [ ] `UserEntity` implements `UserDetails` (id, username, passwordHash, role → GrantedAuthority)
- [ ] `RefreshTokenEntity` với tokenHash + revoked flag
- [ ] `JwtService` với `generateAccessToken()`, `generateRefreshToken()`, `validateAndExtract()`, `isTokenValid()`
- [ ] `UserDetailsServiceImpl` loads user từ DB bằng username
- [ ] `JwtAuthenticationFilter` extract Bearer token → set SecurityContext
- [ ] `SandboxSecurityConfig` với `SessionStateless` + filter registration + `@EnableMethodSecurity`
- [ ] `SandboxAuthController` với `/register`, `/login`, `/refresh` endpoints

### Phase 2 Deliverables
- [ ] `JwtServiceTest`: generate/validate/expired/tampered (≥ 5 test methods)
- [ ] `JwtAuthFilterTest`: valid/missing/invalid Bearer token
- [ ] `RbacIntegrationTest`: USER→ADMIN endpoint → 403 confirmed

### Phase 3 Deliverables (Post-Approval)
- [ ] Production `JwtService` thay `JwtTokenService`
- [ ] Production `SecurityConfig` với JwtFilter + STATELESS
- [ ] Production `AuthService` với BCrypt verify + DB user lookup
- [ ] `RefreshToken` rotation + reuse detection trong production
- [ ] `@PreAuthorize` trên tất cả admin endpoints

---

*Last Updated: 2026-04-03 - Phase 1 + 2 DONE. Phase 3 (production) awaiting "Proceed Phase 3" command*
