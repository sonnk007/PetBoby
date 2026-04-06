package com.sonnk.auth.sandbox.security.config;

import com.sonnk.auth.sandbox.security.filter.JwtAuthenticationFilter;
import com.sonnk.auth.sandbox.security.service.JwtService;
import com.sonnk.auth.sandbox.security.service.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpStatus;

/**
 * Security configuration cho sandbox.security package.
 *
 * Thiết kế để COEXIST với production SecurityConfig:
 * - @Order(1) + securityMatcher("/sandbox/**") → chỉ áp dụng cho paths dưới /sandbox/
 * - Production SecurityConfig (không có @Order, mặc định thấp nhất) → xử lý mọi thứ còn lại
 *
 * Điểm quan trọng:
 * - SessionCreationPolicy.STATELESS: không tạo HTTP session, mọi request phải mang token.
 * - @EnableMethodSecurity: kích hoạt @PreAuthorize, @PostAuthorize trên service/controller.
 * - JwtAuthenticationFilter được đăng ký thủ công (không @Component) để tránh double-registration.
 * - DaoAuthenticationProvider được bind vào chain này để tránh ảnh hưởng production flow.
 */
@Configuration
@EnableMethodSecurity
public class SandboxSecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public SandboxSecurityConfig(UserDetailsServiceImpl userDetailsService,
                                  PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain sandboxSecurityFilterChain(HttpSecurity http,
                                                           JwtAuthenticationFilter jwtAuthFilter)
            throws Exception {
        return http
                // Chỉ chain này xử lý /sandbox/** - production chain xử lý phần còn lại
                .securityMatcher("/sandbox/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(sandboxAuthProvider())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/sandbox/auth/**").permitAll()        // login, register, refresh
                        .requestMatchers("/sandbox/admin/**").hasRole("ADMIN")  // RBAC exercise
                        .anyRequest().authenticated()
                )
                // Trả 401 (không phải 403 mặc định) khi request không có token
                .exceptionHandling(exc -> exc
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Tạo filter thủ công (không @Component) để tránh Spring Boot auto-register vào servlet chain.
     * Khi đó filter chỉ chạy trong Spring Security filter chain, không chạy 2 lần.
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
        return new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    /**
     * DaoAuthenticationProvider: dùng UserDetailsService + PasswordEncoder để xác thực username/password.
     * Bind vào sandbox chain để không ảnh hưởng production SecurityConfig.
     */
    @Bean
    public AuthenticationProvider sandboxAuthProvider() {
        // Spring Security 6.3+: constructor nhận PasswordEncoder thay vì no-arg + setPasswordEncoder
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(passwordEncoder);
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    /**
     * AuthenticationManager: dùng trong SandboxAuthController.login() để authenticate user.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
