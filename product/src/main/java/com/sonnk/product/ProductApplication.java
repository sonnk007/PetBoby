package com.sonnk.product;

import com.sonnk.product.config.ProductAppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot entry point cho module product.
 *
 * - @SpringBootApplication: bật auto-configuration + component scan trong package com.sonnk.product.
 * - @EnableConfigurationProperties: đăng ký ProductAppProperties để Spring bind cấu hình từ application.yml.
 *
 * [PRACTICE] @EntityScan + @EnableJpaRepositories mở rộng scan sang com.example.practice:
 *   - @SpringBootApplication chỉ tự động scan từ package của chính nó (com.sonnk.product).
 *   - Package com.example.practice nằm ngoài cây đó → cần khai báo rõ ràng.
 *   - Thêm cả package gốc "com.sonnk.product" để không mất các entity/repo hiện có.
 *   - Mục đích: cho phép ExerciseEntity được JPA nhận diện và ExerciseRepository hoạt động.
 */
@SpringBootApplication
@EnableConfigurationProperties(ProductAppProperties.class)
@EnableScheduling
@EntityScan({"com.sonnk.product", "com.example.practice"})
@EnableJpaRepositories({"com.sonnk.product", "com.example.practice"})
public class ProductApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductApplication.class, args);
	}

}
