package com.sonnk.product.objects.entity;

import com.sonnk.product.objects.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "topping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Topping extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private BigDecimal price;

    private Boolean active = true;
}
