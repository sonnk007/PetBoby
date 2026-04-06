package com.sonnk.auth.sandbox.security.service;

import com.sonnk.auth.sandbox.security.repository.UserSandboxRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Cầu nối giữa Spring Security và DB người dùng sandbox.
 *
 * Spring Security gọi loadUserByUsername() trong hai trường hợp:
 * 1. AuthenticationManager.authenticate() → khi login (DaoAuthenticationProvider).
 * 2. JwtAuthenticationFilter → khi validate token từ request đến.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserSandboxRepository userRepository;

    public UserDetailsServiceImpl(UserSandboxRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
