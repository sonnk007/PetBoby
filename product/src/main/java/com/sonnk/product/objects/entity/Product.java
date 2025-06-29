package com.sonnk.product.objects.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonnk.product.objects.dtos.ToppingInfo;
import com.sonnk.product.objects.entity.base.BaseEntity;
import com.sonnk.product.utils.enums.ProductSize;
import com.sonnk.product.utils.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(length = 1000)
    private String description;

    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private ProductSize productSize;

    private Boolean hasTopping;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(columnDefinition = "TEXT")
    private String toppings;

    @Column(length = 1000)
    private String imageUrl;

    // Jackson ObjectMapper, có thể khai báo static để tiết kiệm
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Getter chuyển từ JSON String sang List<ToppingInfo>
    public List<ToppingInfo> getToppings() {
        if (toppings == null || toppings.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(toppings, new TypeReference<List<ToppingInfo>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    // Setter chuyển từ List<ToppingInfo> sang JSON String
    public void setToppings(List<ToppingInfo> toppings) {
        try {
            this.toppings = objectMapper.writeValueAsString(toppings);
        } catch (Exception e) {
            e.printStackTrace();
            this.toppings = "[]";
        }
    }
}
