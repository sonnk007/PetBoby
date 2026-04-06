package com.sonnk.auth;

import com.sonnk.auth.config.AuthJwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Entry point cho auth service.
 *
 * Nhiệm vụ chính của service này:
 * - Xử lý login/logout.
 * - Sinh và validate JWT cho các client.
 * - Làm đầu mối cho xác thực + phân quyền (authN/authZ) trong kiến trúc microservices của PetBoby.
 *
 * Lưu ý:
 * - Service này không trực tiếp thao tác DB user (bounded context user).
 * - Sau này sẽ gọi user-service qua HTTP để verify credential / lấy thông tin role.
 */
@SpringBootApplication
@EnableConfigurationProperties(AuthJwtProperties.class)
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}

