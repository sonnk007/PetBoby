package com.sonnk.user.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "customer")
@Getter
@Setter
public class CustomerModel extends UserModel {

    /**
     * Điểm tích luỹ của khách hàng.
     */
    @Column(precision = 16, scale = 2, nullable = false)
    private BigDecimal loyaltyPoints = BigDecimal.ZERO;

    @Column(length = 50)
    private String membershipLevel;
}
