package com.sonnk.order.demo;

import com.sonnk.order.model.entity.Order;
import com.sonnk.order.model.entity.OrderItem;
import com.sonnk.order.model.entity.OrderItemTopping;
import com.sonnk.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service DEMO để minh hoạ vấn đề N+1 query trong JPA
 * và cách tránh bằng fetch join.
 *
 * CHỈ dùng cho mục đích học tập, không tham gia luồng nghiệp vụ chính.
 */
@Service
public class OrderNPlusOneDemoService {

    private final OrderRepository orderRepository;

    public OrderNPlusOneDemoService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Cách load NAIVE dễ gây N+1:
     * - Gọi repository trả về danh sách Order (chỉ load bảng orders).
     * - Sau đó lặp và truy cập o.getItems(), item.getToppings() trong code Java.
     * - Với fetch = LAZY, mỗi lần truy cập sẽ bắn thêm 1 query → N+1.
     */
    @Transactional(readOnly = true)
    public List<Order> loadOrdersNaive(String branchCode, LocalDateTime from, LocalDateTime to) {
        List<Order> orders = orderRepository.findByBranchCodeAndCreatedAtBetween(branchCode, from, to);
        touchItemsAndToppings(orders);
        return orders;
    }

    /**
     * Cách load đã tối ưu hơn:
     * - Dùng fetch join tại repository để lấy kèm OrderItem + OrderItemTopping
     *   trong cùng (hoặc rất ít) query.
     * - Sau đó lặp trên graph đã được load sẵn, hạn chế N+1.
     */
    @Transactional(readOnly = true)
    public List<Order> loadOrdersOptimized(String branchCode, LocalDateTime from, LocalDateTime to) {
        List<Order> orders =
                orderRepository.findWithItemsAndToppingsByBranchCodeAndCreatedAtBetween(branchCode, from, to);
        touchItemsAndToppings(orders);
        return orders;
    }

    /**
     * Helper dùng chung cho cả 2 luồng demo để:
     * - Truy cập vào collection LAZY nhằm buộc JPA load dữ liệu.
     * - Đồng thời tránh cảnh báo "unused variable" của linter.
     */
    private void touchItemsAndToppings(List<Order> orders) {
        int total = 0;
        for (Order order : orders) {
            List<OrderItem> items = order.getItems();
            for (OrderItem item : items) {
                List<OrderItemTopping> toppings = item.getToppings();
                total += toppings.size();
            }
        }
        // Giá trị total chỉ dùng để tránh tối ưu hoá quá mức; không có ý nghĩa nghiệp vụ.
        if (total < 0) {
            throw new IllegalStateException("This should never happen, demo-only guard.");
        }
    }
}

