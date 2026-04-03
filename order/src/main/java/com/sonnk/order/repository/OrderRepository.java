package com.sonnk.order.repository;

import com.sonnk.order.model.entity.Order;
import com.sonnk.order.model.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    /**
     * [N+1 FIX] Lấy một Order kèm toàn bộ items và toppings trong MỘT câu SQL duy nhất.
     *
     * Vấn đề không có method này:
     *   - findById(id) chỉ SELECT bảng orders.
     *   - Khi toResponse() gọi order.getItems() → 1 query thêm.
     *   - Khi toResponse() gọi item.getToppings() cho mỗi item → N query thêm.
     *   → Tổng: 1 + 1 + N queries (N+1 pattern).
     *
     * Giải pháp:
     *   - left join fetch o.items → JOIN orders JOIN order_item trong cùng query.
     *   - left join fetch i.toppings → JOIN thêm order_item_topping.
     *   - Chỉ 1 SQL SELECT duy nhất, giảm round-trip tới DB.
     *
     * Lưu ý Cartesian Product:
     *   - Đây là đơn lẻ (by id), kết quả luôn là 0 hoặc 1 Order.
     *   - Dùng "distinct" để JPA de-duplicate kết quả Java object sau JOIN.
     *   - KHÔNG nên dùng pattern này cho danh sách lớn vì sẽ gây in-memory pagination.
     */
    @Query("""
           select distinct o
           from Order o
           left join fetch o.items i
           left join fetch i.toppings
           where o.id = :id
           """)
    Optional<Order> findByIdWithItemsAndToppings(@Param("id") Long id);

    /**
     * [PAGINATION FIX] Lấy danh sách Order có phân trang.
     *
     * Tại sao KHÔNG dùng JOIN FETCH ở đây?
     *   - Order.items là @OneToMany (collection). JOIN FETCH + Pageable trên collection
     *     sẽ khiến Hibernate tải TẤT CẢ dữ liệu vào bộ nhớ rồi mới phân trang
     *     (in-memory pagination) → HHH90003004 warning, nguy cơ OutOfMemoryError.
     *
     * Giải pháp thay thế:
     *   - Dùng findAll(Pageable) → SQL LIMIT/OFFSET đúng nghĩa tại DB.
     *   - Kết hợp hibernate.default_batch_fetch_size=50 (đã cấu hình trong yml):
     *     khi truy cập items của N orders, Hibernate gom thành 1 query với IN(...50 ids...)
     *     thay vì N query riêng lẻ → N+1 giảm xuống còn N/50 + 1 queries.
     *
     * countQuery tách riêng để tránh JOIN không cần thiết khi đếm tổng số bản ghi.
     */
    @Query(value = "select o from Order o",
           countQuery = "select count(o) from Order o")
    Page<Order> findAllPaged(Pageable pageable);
}

