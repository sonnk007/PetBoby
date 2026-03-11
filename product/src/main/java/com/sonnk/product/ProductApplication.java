package com.sonnk.product;

import com.sonnk.product.config.ProductAppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot entry point cho module product.
 *
 * - @SpringBootApplication: bật auto-configuration + component scan trong package com.sonnk.product.
 * - @EnableConfigurationProperties: đăng ký ProductAppProperties để Spring bind cấu hình từ application.yml.
 */
@SpringBootApplication
@EnableConfigurationProperties(ProductAppProperties.class)
@EnableScheduling
public class ProductApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductApplication.class, args);
	}

}
