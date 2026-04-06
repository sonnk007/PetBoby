package com.sonnk.auth.sandbox.security.filter;

import com.sonnk.auth.sandbox.security.service.JwtService;
import com.sonnk.auth.sandbox.security.service.UserDetailsServiceImpl;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter chặn mọi request đến sandbox endpoints, trích xuất Bearer token và xác thực.
 *
 * Luồng xử lý:
 * 1. Đọc header "Authorization: Bearer <token>".
 * 2. Nếu không có → tiếp tục chain (Spring Security sẽ trả 401 nếu endpoint cần auth).
 * 3. Nếu có → extract username từ token, load UserDetails, validate, set SecurityContext.
 * 4. Token không hợp lệ → không set auth → Spring Security trả 401 tự động.
 *
 * KHÔNG dùng @Component để tránh Spring Boot auto-register vào servlet filter chain ngoài
 * Spring Security. Filter này được đăng ký thủ công trong SandboxSecurityConfig.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsServiceImpl userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            // Không có token → tiếp tục; Spring Security sẽ reject nếu endpoint cần xác thực
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            String username = jwtService.extractUsername(token);

            // Chỉ set authentication nếu chưa có trong context (tránh ghi đè)
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(token, userDetails)) {
                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException | UsernameNotFoundException e) {
            // Token không hợp lệ hoặc user không tồn tại.
            // Không set authentication → Spring Security sẽ tự trả 401.
            // Không log chi tiết để tránh lộ thông tin trong production.
        }

        filterChain.doFilter(request, response);
    }
}
