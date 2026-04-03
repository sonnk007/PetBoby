package com.sonnk.order.application.service;

import com.sonnk.order.application.dto.ProductInfoDto;
import com.sonnk.order.application.exception.ProductNotFoundException;
import com.sonnk.order.application.event.OrderCreatedEvent;
import com.sonnk.order.application.event.saga.SagaOrderCreatedEvent;
import com.sonnk.order.application.port.OrderEventPublisher;
import com.sonnk.order.application.port.ProductClient;
import com.sonnk.order.api.dto.CreateOrderRequest;
import com.sonnk.order.api.dto.OrderItemRequest;
import com.sonnk.order.api.dto.OrderItemResponse;
import com.sonnk.order.api.dto.OrderResponse;
import com.sonnk.order.model.entity.Order;
import com.sonnk.order.model.entity.OrderItem;
import com.sonnk.order.model.entity.enums.OrderSagaState;
import com.sonnk.order.model.entity.enums.OrderStatus;
import com.sonnk.order.infrastructure.messaging.SagaEventPublisher;
import com.sonnk.order.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service cho use case Order (Tuần 3 – tích hợp gọi Product service).
 *
 * Công dụng:
 * - createOrder: gọi ProductClient.getProductsByIds() để lấy giá/tên sản phẩm (1 request bulk thay vì N request).
 * - Transaction boundary tại đây: toàn bộ tạo Order + OrderItems trong 1 transaction.
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final OrderEventPublisher orderEventPublisher;
    private final SagaEventPublisher sagaEventPublisher;

    public OrderService(OrderRepository orderRepository, ProductClient productClient,
                       OrderEventPublisher orderEventPublisher,
                       SagaEventPublisher sagaEventPublisher) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
        this.orderEventPublisher = orderEventPublisher;
        this.sagaEventPublisher = sagaEventPublisher;
    }

    /**
     * Tạo đơn hàng: validate product qua Product service, snapshot giá/tên rồi lưu.
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        List<Long> productIds = request.items().stream()
                .map(OrderItemRequest::productId)
                .distinct()
                .toList();

        List<ProductInfoDto> products = productClient.getProductsByIds(productIds);
        Map<Long, ProductInfoDto> productMap = products.stream().collect(Collectors.toMap(ProductInfoDto::id, p -> p));

        for (OrderItemRequest item : request.items()) {
            if (!productMap.containsKey(item.productId())) {
                throw new ProductNotFoundException("Product not found or inactive: " + item.productId());
            }
        }

        Order order = new Order();
        order.setOrderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setBranchCode(request.branchCode());
        order.setCustomerId(request.customerId());
        order.setPaymentMethod(request.paymentMethod());
        order.setStatus(OrderStatus.NEW);
        order.setSagaState(OrderSagaState.INVENTORY_PENDING);
        order.setTotalDiscount(BigDecimal.ZERO);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest req : request.items()) {
            ProductInfoDto product = productMap.get(req.productId());
            BigDecimal unitPrice = product.price();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(req.quantity()));
            totalAmount = totalAmount.add(lineTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(product.id());
            orderItem.setProductName(product.name());
            orderItem.setProductSize(product.productSize() != null ? product.productSize() : "");
            orderItem.setUnitPrice(unitPrice);
            orderItem.setQuantity(req.quantity());
            orderItem.setLineTotal(lineTotal);
            orderItems.add(orderItem);
        }

        order.setTotalAmount(totalAmount);
        order.setFinalAmount(totalAmount.subtract(order.getTotalDiscount()));
        order.setItems(orderItems);
        Order saved = orderRepository.save(order);

        // Tuần 4 – Kafka: publish event bất đồng bộ; eventId dùng cho idempotent consumer (xem docs/kafka-data-integrity-and-deep-dive.md).
        orderEventPublisher.publishOrderCreated(OrderCreatedEvent.of(
                saved.getId(),
                saved.getOrderCode(),
                saved.getBranchCode(),
                saved.getCustomerId(),
                saved.getTotalAmount(),
                saved.getFinalAmount(),
                saved.getCreatedAt()
        ));
        // Saga choreography skeleton: khoi tao event buoc 1 (inventory reserve).
        sagaEventPublisher.publishSagaStarted(
                SagaOrderCreatedEvent.of(
                        saved.getId(),
                        saved.getOrderCode(),
                        saved.getBranchCode(),
                        saved.getFinalAmount(),
                        saved.getCreatedAt()
                )
        );

        return toResponse(saved);
    }

    /**
     * [PAGINATION FIX] Trả về Page thay vì List để tránh tải toàn bộ dữ liệu vào bộ nhớ.
     *
     * Cách cũ: findAll() → SELECT * FROM orders (không giới hạn) → OOM risk nếu bảng lớn.
     * Cách mới: findAllPaged(pageable) → SELECT ... LIMIT ? OFFSET ? → chỉ lấy đúng trang cần.
     *
     * Về N+1 với collection:
     *   - KHÔNG thể dùng JOIN FETCH ở đây vì Order.items là @OneToMany.
     *     JOIN FETCH + Pageable trên collection → Hibernate tải ALL vào bộ nhớ rồi cắt trang
     *     (in-memory pagination, HHH90003004 warning).
     *   - Thay vào đó: dùng default_batch_fetch_size=50 (đã cấu hình trong yml).
     *     Hibernate sẽ gom lazy loading thành: SELECT ... WHERE order_id IN (id1,...,id50)
     *     → Trang 20 bản ghi = 1 query lấy orders + 1 query batch-load items = 2 queries tổng.
     *
     * Dirty Checking lưu ý:
     *   - @Transactional(readOnly=true) → Hibernate tắt dirty checking snapshot,
     *     tiết kiệm memory đáng kể khi trang chứa nhiều entity.
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> listOrders(Pageable pageable) {
        return orderRepository.findAllPaged(pageable).map(this::toResponse);
    }

    /**
     * [N+1 FIX] Lấy Order kèm items + toppings trong 1 SQL SELECT duy nhất.
     *
     * Cách cũ: findById(id) → 1 query (orders only)
     *   → toResponse() gọi order.getItems() → 1 query thêm (order_item)
     *   → toResponse() gọi item.getToppings() cho mỗi item → N query (order_item_topping)
     *   = Tổng: 2 + N queries.
     *
     * Cách mới: findByIdWithItemsAndToppings(id) → 1 query duy nhất với LEFT JOIN FETCH
     *   = Tổng: 1 query.
     */
    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        Order order = orderRepository.findByIdWithItemsAndToppings(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Order not found: " + id));
        return toResponse(order);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getId(),
                        i.getProductId(),
                        i.getProductName(),
                        i.getProductSize(),
                        i.getUnitPrice(),
                        i.getQuantity(),
                        i.getLineTotal()
                ))
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getOrderCode(),
                order.getBranchCode(),
                order.getCustomerId(),
                order.getStatus(),
                order.getSagaState(),
                order.getPaymentMethod(),
                order.getTotalAmount(),
                order.getTotalDiscount(),
                order.getFinalAmount(),
                order.getCreatedAt(),
                itemResponses
        );
    }
}
