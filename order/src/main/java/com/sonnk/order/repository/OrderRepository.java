package com.sonnk.order.repository;

import com.sonnk.order.model.entity.Order;
import com.sonnk.order.model.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByBranchCodeAndCreatedAtBetween(String branchCode, LocalDateTime from, LocalDateTime to);

    List<Order> findByStatus(OrderStatus status);
}

