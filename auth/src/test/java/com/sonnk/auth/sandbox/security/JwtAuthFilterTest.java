package com.sonnk.auth.sandbox.security;

import com.sonnk.auth.config.AuthJwtProperties;
import com.sonnk.auth.sandbox.security.entity.SandboxRole;
import com.sonnk.auth.sandbox.security.entity.UserEntity;
import com.sonnk.auth.sandbox.security.filter.JwtAuthenticationFilter;
import com.sonnk.auth.sandbox.security.service.JwtService;
import com.sonnk.auth.sandbox.security.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho JwtAuthenticationFilter.
 *
 * Kiểm tra 3 trường hợp cốt lõi:
 * 1. Valid Bearer token → SecurityContext được set với Authentication.
 * 2. Thiếu Authorization header → chain tiếp tục, không set auth.
 * 3. Token không hợp lệ (tampered) → chain tiếp tục, không set auth.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private UserDetailsServiceImpl userDetailsService;
    @Mock
    private FilterChain filterChain;

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        AuthJwtProperties props = new AuthJwtProperties();
        props.setSecret("test-secret-key-for-unit-testing!!");
        props.setAccessTokenTtlSeconds(3600);
        props.setRefreshTokenTtlSeconds(86400);
        jwtService = new JwtService(props);
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        // Xóa SecurityContext sau mỗi test
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_validBearerToken_setsAuthentication() throws Exception {
        String token = jwtService.generateAccessToken("alice", java.util.List.of("ROLE_USER"));

        UserEntity mockUser = new UserEntity();
        mockUser.setUsername("alice");
        mockUser.setPasswordHash("hashed");
        mockUser.setRole(SandboxRole.USER);
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(mockUser);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        // SecurityContext phải có authentication
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("alice");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_missingAuthorizationHeader_doesNotSetAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // Không set Authorization header
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_invalidToken_doesNotSetAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer this.is.not.valid.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        // Token không hợp lệ → không set auth, filter chain vẫn tiếp tục
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_bearerPrefixWithoutToken_doesNotSetAuthentication() throws Exception {
        String token = jwtService.generateAccessToken("alice", java.util.List.of("ROLE_USER"));
        // Tamper signature
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tampered);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
