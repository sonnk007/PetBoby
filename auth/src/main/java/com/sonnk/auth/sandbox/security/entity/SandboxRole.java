package com.sonnk.auth.sandbox.security.entity;

/**
 * Role đơn giản cho sandbox. Ánh xạ sang Spring GrantedAuthority với prefix ROLE_.
 * Sản xuất dùng UserRole trong user-service (CUSTOMER, EMPLOYEE, MANAGER, ADMIN).
 */
public enum SandboxRole {
    USER,
    ADMIN
}
