package com.sonnk.product.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Demo Spring Boot fundamentals:
 * - Externalized configuration với @ConfigurationProperties.
 * - Nhóm các setting liên quan đến module product vào một chỗ type-safe.
 *
 * Các thuộc tính được map từ prefix: petboby.product
 * Ví dụ trong application.yml:
 *
 * petboby:
 *   product:
 *     default-page-size: 20
 *     max-toppings-per-drink: 5
 */
@ConfigurationProperties(prefix = "petboby.product")
public class ProductAppProperties {

    /**
     * Số bản ghi mặc định khi phân trang danh sách sản phẩm.
     */
    private int defaultPageSize = 20;

    /**
     * Số topping tối đa cho một đồ uống (hạn chế logic menu).
     */
    private int maxToppingsPerDrink = 5;

    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    public void setDefaultPageSize(int defaultPageSize) {
        this.defaultPageSize = defaultPageSize;
    }

    public int getMaxToppingsPerDrink() {
        return maxToppingsPerDrink;
    }

    public void setMaxToppingsPerDrink(int maxToppingsPerDrink) {
        this.maxToppingsPerDrink = maxToppingsPerDrink;
    }
}

