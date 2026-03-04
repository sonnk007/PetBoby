package com.sonnk.order.model.entity;

import com.sonnk.order.model.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "order_item_topping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemTopping extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    /**
     * Snapshot topping tại thời điểm tạo đơn.
     */
    @Column(nullable = false)
    private Long toppingId;

    @Column(nullable = false, length = 150)
    private String toppingName;

    @Column(precision = 16, scale = 2, nullable = false)
    private BigDecimal toppingPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(precision = 16, scale = 2, nullable = false)
    private BigDecimal lineTotal;
}

