package com.sonnk.sandbox.db;

import com.sonnk.order.model.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderN1FixRepository extends JpaRepository<Order, Long> {

    /**
     * [N+1 IDENTIFIED]
     * findAll triggers N queries for items even with batch_fetch_size (multi-query).
     */
    @Override
    Page<Order> findAll(Pageable pageable);

    /**
     * [N+1 FIXED via EntityGraph]
     * Uses EntityGraph to fetch items in the same query context.
     * Note: Be careful with pagination on collections!
     */
    @EntityGraph(attributePaths = {"items"})
    Page<Order> findAllWithItems(Pageable pageable);
}
