package com.sonnk.auth.sandbox.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonnk.auth.sandbox.security.api.dto.RegisterRequest;
import com.sonnk.auth.sandbox.security.api.dto.SandboxLoginRequest;
import com.sonnk.auth.sandbox.security.entity.SandboxRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test chứng minh RBAC hoạt động đúng.
 *
 * Exercise chính:
 *   USER token → /sandbox/admin/test → 403 Forbidden
 *   ADMIN token → /sandbox/admin/test → 200 OK
 *
 * Test này khởi động Spring context thực sự (H2 DB, filter chain, security config)
 * để xác nhận toàn bộ luồng từ register → login → truy cập endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userAccessToken;
    private String adminAccessToken;

    @BeforeEach
    void setUp() throws Exception {
        // Đăng ký user với role USER
        RegisterRequest userReg = new RegisterRequest("testuser_rbac", "password123", SandboxRole.USER);
        mockMvc.perform(post("/sandbox/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userReg)));
        // Ignore 409 nếu đã tồn tại từ test trước

        // Đăng ký user với role ADMIN
        RegisterRequest adminReg = new RegisterRequest("testadmin_rbac", "password123", SandboxRole.ADMIN);
        mockMvc.perform(post("/sandbox/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminReg)));

        // Login USER → lấy access token
        userAccessToken = loginAndGetToken("testuser_rbac", "password123");

        // Login ADMIN → lấy access token
        adminAccessToken = loginAndGetToken("testadmin_rbac", "password123");
    }

    // ---- RBAC Core Exercise ----

    /**
     * ⛔ USER token → ADMIN endpoint → 403 Forbidden
     * Đây là exercise chính: @PreAuthorize("hasRole('ADMIN')") từ chối ROLE_USER.
     */
    @Test
    void userToken_accessAdminEndpoint_returns403() throws Exception {
        mockMvc.perform(get("/sandbox/admin/test")
                        .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().isForbidden());  // 403
    }

    /**
     * ✅ ADMIN token → ADMIN endpoint → 200 OK
     */
    @Test
    void adminToken_accessAdminEndpoint_returns200() throws Exception {
        mockMvc.perform(get("/sandbox/admin/test")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().isOk());  // 200
    }

    /**
     * ⛔ Không có token → protected endpoint → 401 Unauthorized
     */
    @Test
    void noToken_accessProtectedEndpoint_returns401() throws Exception {
        mockMvc.perform(get("/sandbox/protected"))
                .andExpect(status().isUnauthorized());  // 401
    }

    /**
     * ⛔ Không có token → ADMIN endpoint → 401 Unauthorized
     */
    @Test
    void noToken_accessAdminEndpoint_returns401() throws Exception {
        mockMvc.perform(get("/sandbox/admin/test"))
                .andExpect(status().isUnauthorized());  // 401
    }

    /**
     * ✅ USER token → /sandbox/protected → 200 OK (không phải ADMIN endpoint)
     */
    @Test
    void userToken_accessProtectedEndpoint_returns200() throws Exception {
        mockMvc.perform(get("/sandbox/protected")
                        .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().isOk());  // 200
    }

    // ---- Helper ----

    private String loginAndGetToken(String username, String password) throws Exception {
        SandboxLoginRequest loginRequest = new SandboxLoginRequest(username, password);
        MvcResult result = mockMvc.perform(post("/sandbox/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        // Extract accessToken từ JSON response
        return objectMapper.readTree(body).get("accessToken").asText();
    }
}
