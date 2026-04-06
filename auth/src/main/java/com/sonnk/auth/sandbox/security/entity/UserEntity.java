package com.sonnk.auth.sandbox.security.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Entity người dùng trong sandbox.
 *
 * Implements UserDetails để Spring Security có thể dùng trực tiếp trong authentication flow.
 * Bảng: sandbox_users (H2 in-memory, không ảnh hưởng production schema).
 *
 * Điểm quan trọng:
 * - getAuthorities() trả về role với prefix ROLE_ → @PreAuthorize("hasRole('ADMIN')") hoạt động đúng.
 * - passwordHash lưu BCrypt hash, không bao giờ lưu plain-text.
 */
@Entity
@Table(name = "sandbox_users")
public class UserEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SandboxRole role;

    // ---- UserDetails interface ----

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring expects "ROLE_ADMIN", "ROLE_USER" for hasRole() checks
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }

    // ---- Getters/Setters ----

    public Long getId() { return id; }

    public void setUsername(String username) { this.username = username; }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public SandboxRole getRole() { return role; }

    public void setRole(SandboxRole role) { this.role = role; }
}
