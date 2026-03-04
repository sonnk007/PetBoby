package com.sonnk.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình gọi Product service (synchronous HTTP).
 * Dùng cho RestTemplate/WebClient: base URL và timeout.
 *
 * Lợi ích: tách config ra file, dễ đổi theo môi trường (dev/prod).
 * Trade-off: phải set đúng base-url khi chạy nhiều service (localhost vs gateway).
 */
@ConfigurationProperties(prefix = "petboby.order.product-service")
public class ProductServiceProperties {

    /**
     * Base URL của product service (vd: http://localhost:8082 khi chạy local, hoặc http://product khi dùng Docker).
     */
    private String baseUrl = "http://localhost:8082";

    /**
     * Timeout kết nối (ms). RestTemplate sẽ ném exception nếu không kết nối được trong khoảng này.
     */
    private int connectTimeoutMs = 2_000;

    /**
     * Timeout đọc response (ms). Tránh block vô hạn khi product service chậm.
     */
    private int readTimeoutMs = 5_000;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
}
