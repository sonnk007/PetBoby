package com.sonnk.order.repository;

import com.sonnk.order.model.entity.Order;
import com.sonnk.order.model.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository truy cập Order (JPA).
 * Derived query: By + tên property (BranchCode, Status, CreatedAtBetween) tạo điều kiện WHERE.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * find… → SELECT; By BranchCode And CreatedAtBetween → WHERE branch_code = ? AND created_at BETWEEN ? AND ?.
     * Lấy đơn theo chi nhánh và khoảng thời gian tạo (báo cáo doanh thu theo branch/ngày).
     */
    List<Order> findByBranchCodeAndCreatedAtBetween(String branchCode, LocalDateTime from, LocalDateTime to);

    /**
     * By Status → WHERE status = ?. Lấy tất cả đơn theo trạng thái (vd: NEW, CONFIRMED, COMPLETED).
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Demo tránh N+1 query:
     * - Khi load Order kèm OrderItem và OrderItemTopping, nếu để LAZY mà lặp trong code Java,
     *   JPA sẽ phát sinh rất nhiều query con (N+1).
     * - Sử dụng JOIN FETCH để lấy luôn toàn bộ graph dữ liệu trong ít query hơn.
     *
     * Lưu ý:
     * - Đây là method phục vụ demo/learning, không phải báo cáo tối ưu cuối cùng.
     */
    @Query("""
           select distinct o
           from Order o
           left join fetch o.items i
           left join fetch i.toppings t
           where o.branchCode = :branchCode
             and o.createdAt between :from and :to
           """)
    List<Order> findWithItemsAndToppingsByBranchCodeAndCreatedAtBetween(
            @Param("branchCode") String branchCode,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}

