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

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders() {
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        Order order = orderRepository.findById(id)
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
