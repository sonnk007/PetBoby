package com.sonnk.order.api;

import com.sonnk.order.api.dto.CreateOrderRequest;
import com.sonnk.order.api.dto.OrderResponse;
import com.sonnk.order.application.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * REST API cho đơn hàng (Tuần 3 – Order service gọi Product service để validate và lấy giá).
 * POST /api/orders → tạo đơn (gọi Product bulk API); GET /api/orders, GET /api/orders/{id}.
 *
 * [PAGINATION] GET /api/orders?page=0&size=20 trả về Page<OrderResponse>:
 *   {
 *     "content": [...],
 *     "totalElements": 500,
 *     "totalPages": 25,
 *     "size": 20,
 *     "number": 0
 *   }
 * Tại sao cần? Không giới hạn kết quả → SELECT * không LIMIT → tải hàng nghìn bản ghi
 * vào JVM heap → OutOfMemoryError + response chậm.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    // Giới hạn page size tối đa để tránh client request size=999999
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse created = orderService.createOrder(request);
        return ResponseEntity
                .created(URI.create("/api/orders/" + created.id()))
                .body(created);
    }

    /**
     * [PAGINATION] Danh sách đơn hàng có phân trang.
     *
     * Query params:
     *   - page: số trang (0-based, default = 0)
     *   - size: số bản ghi mỗi trang (default = 20, max = 100)
     *
     * Ví dụ: GET /api/orders?page=0&size=20
     *
     * Sắp xếp: mặc định theo createdAt DESC (đơn mới nhất lên đầu).
     * Giới hạn size tối đa tại controller để phòng tham số độc hại từ client.
     */
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orderService.listOrders(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(id));
    }
}
