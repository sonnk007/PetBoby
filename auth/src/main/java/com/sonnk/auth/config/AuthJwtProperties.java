package com.sonnk.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình JWT cho auth service (externalized configuration).
 *
 * - secret: khoá HMAC dùng ký JWT (demo: để trong config; thực tế nên dùng vault/secret manager).
 * - issuer: tên hệ thống (vd: petboby-auth).
 * - accessTokenTtlSeconds: thời gian sống của access token (giây).
 * - refreshTokenTtlSeconds: thời gian sống của refresh token (giây).
 */
@ConfigurationProperties(prefix = "petboby.auth.jwt")
public class AuthJwtProperties {

    private String secret;
    private String issuer = "petboby-auth";
    private long accessTokenTtlSeconds = 3600;
    private long refreshTokenTtlSeconds = 2592000; // 30 ngày (demo), sản xuất điều chỉnh qua config

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    public void setAccessTokenTtlSeconds(long accessTokenTtlSeconds) {
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public long getRefreshTokenTtlSeconds() {
        return refreshTokenTtlSeconds;
    }

    public void setRefreshTokenTtlSeconds(long refreshTokenTtlSeconds) {
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }
}

