package com.sonnk.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonnk.auth.config.AuthJwtProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service sinh JWT HMAC-SHA256 đơn giản, không phụ thuộc thư viện ngoài.
 *
 * Chỉ dùng cho mục đích học tập:
 * - Minh hoạ cấu trúc JWT: header.payload.signature (Base64Url).
 * - Cách ký với khoá bí mật (HMAC-SHA256).
 *
 * Khi lên production:
 * - Nên dùng thư viện chuẩn (Spring Security JWT, Nimbus, jjwt, ...).
 * - Nên lưu secret/key trong secret manager, không hard-code trong repo.
 */
@Service
public class JwtTokenService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final AuthJwtProperties properties;
    private final ObjectMapper objectMapper;

    public JwtTokenService(AuthJwtProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String generateAccessToken(String subject, List<String> roles) {
        try {
            String headerJson = objectMapper.writeValueAsString(Map.of(
                    "alg", "HS256",
                    "typ", "JWT"
            ));

            long nowSeconds = Instant.now().getEpochSecond();
            long expSeconds = nowSeconds + properties.getAccessTokenTtlSeconds();

            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", subject);
            payload.put("roles", roles);
            payload.put("iss", properties.getIssuer());
            payload.put("iat", nowSeconds);
            payload.put("exp", expSeconds);

            String payloadJson = objectMapper.writeValueAsString(payload);

            String headerB64 = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
            String payloadB64 = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));

            String toSign = headerB64 + "." + payloadB64;
            String signature = sign(toSign, properties.getSecret());

            return toSign + "." + signature;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JWT", e);
        }
    }

    private String sign(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

