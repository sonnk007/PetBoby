package com.sonnk.order.demo;

import com.sonnk.order.model.entity.Order;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST API DEMO cho JPA N+1.
 *
 * - Không thuộc luồng nghiệp vụ chính của hệ thống.
 * - Dùng để bạn bật log SQL và so sánh số lượng query giữa:
 *   + /api/demo/orders/nplus1/naive
 *   + /api/demo/orders/nplus1/optimized
 */
@RestController
@RequestMapping("/api/demo/orders/nplus1")
public class OrderNPlusOneDemoController {

    private final OrderNPlusOneDemoService demoService;

    public OrderNPlusOneDemoController(OrderNPlusOneDemoService demoService) {
        this.demoService = demoService;
    }

    /**
     * Cách dùng:
     * - Gọi endpoint với khoảng thời gian có đủ dữ liệu order để thấy khác biệt.
     * - Ví dụ:
     *   GET /api/demo/orders/nplus1/naive?branchCode=HN-CauGiay-01&from=2025-01-01T00:00:00&to=2025-12-31T23:59:59
     * - Bật show-sql để đếm số query.
     */
    @GetMapping("/naive")
    public ResponseEntity<List<OrderSummary>> naive(
            @RequestParam String branchCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        List<Order> orders = demoService.loadOrdersNaive(branchCode, from, to);
        return ResponseEntity.ok(orders.stream().map(OrderSummary::from).toList());
    }

    /**
     * Endpoint tối ưu hơn, dùng fetch join.
     * - Gọi tương tự như /naive, sau đó so sánh số query trong log SQL.
     */
    @GetMapping("/optimized")
    public ResponseEntity<List<OrderSummary>> optimized(
            @RequestParam String branchCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        List<Order> orders = demoService.loadOrdersOptimized(branchCode, from, to);
        return ResponseEntity.ok(orders.stream().map(OrderSummary::from).toList());
    }

    /**
     * DTO tóm tắt để trả ra client.
     * - Không trả full graph để tránh lộ cấu trúc nội bộ demo.
     */
    public record OrderSummary(
            Long orderId,
            String orderCode,
            String branchCode,
            int itemCount,
            int toppingCount
    ) {
        public static OrderSummary from(Order order) {
            int itemCount = order.getItems().size();
            int toppingCount = order.getItems().stream()
                    .mapToInt(item -> item.getToppings().size())
                    .sum();
            return new OrderSummary(
                    order.getId(),
                    order.getOrderCode(),
                    order.getBranchCode(),
                    itemCount,
                    toppingCount
            );
        }
    }
}

