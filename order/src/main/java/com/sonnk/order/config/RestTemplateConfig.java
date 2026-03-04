package com.sonnk.order.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Cấu hình RestTemplate với timeout rõ ràng (Tuần 3 – Microservices communication).
 *
 * Công dụng:
 * - Connect timeout: tránh block lâu khi product service không phản hồi (network/startup).
 * - Read timeout: tránh chờ response vô hạn khi product service xử lý chậm.
 * Trade-off: RestTemplate là blocking; nếu cần non-blocking dùng WebClient (spring-webflux).
 */
@Configuration
@EnableConfigurationProperties(ProductServiceProperties.class)
public class RestTemplateConfig {

    /**
     * RestTemplate dùng chung để gọi Product service.
     * Base URL lấy từ ProductServiceProperties trong ProductRestClient (không có rootUri trên RestTemplate).
     */
    @Bean
    public RestTemplate productServiceRestTemplate(ProductServiceProperties props) {
        return new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(props.getReadTimeoutMs()))
                .build();
    }
}
