package com.app.ecom.service;

import com.app.ecom.dto.CartItemResponse;
import com.app.ecom.dto.AddressDTO;
import com.app.ecom.dto.OrderCreateRequest;
import com.app.ecom.dto.OrderDetailResponse;
import com.app.ecom.dto.OrderItemDTO;
import com.app.ecom.dto.OrderResponse;
import com.app.ecom.dto.OrderStatusTimelineEntry;
import com.app.ecom.dto.OrderStatusUpdateRequest;
import com.app.ecom.dto.OrderSummaryResponse;
import com.app.ecom.dto.PaymentStatusTimelineEntry;
import com.app.ecom.dto.PromoApplyResponse;
import com.app.ecom.exception.InsufficientStockException;
import com.app.ecom.exception.ResourceNotFoundException;
import com.app.ecom.model.Address;
import com.app.ecom.model.Order;
import com.app.ecom.model.OrderItem;
import com.app.ecom.model.OrderStatus;
import com.app.ecom.model.NotificationType;
import com.app.ecom.model.OrderStatusHistory;
import com.app.ecom.model.Payment;
import com.app.ecom.model.PaymentStatus;
import com.app.ecom.model.Product;
import com.app.ecom.model.User;
import com.app.ecom.repository.OrderRepository;
import com.app.ecom.repository.OrderStatusHistoryRepository;
import com.app.ecom.repository.PaymentRepository;
import com.app.ecom.repository.PaymentStatusHistoryRepository;
import com.app.ecom.repository.ProductRepository;
import com.app.ecom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final PaymentStatusHistoryRepository paymentStatusHistoryRepository;
    private final PromoCodeService promoCodeService;
    private final NotificationService notificationService;

    @Transactional
    public OrderResponse createOrder(Long userId, OrderCreateRequest request, String idempotencyKey) {
        String normalizedKey = idempotencyKey != null ? idempotencyKey.trim() : null;
        log.info("Creating order for userId={}, idempotencyKey={}", userId, normalizedKey);

        if (normalizedKey != null && !normalizedKey.isBlank()) {
            return orderRepository.findByUserIdAndIdempotencyKey(userId, normalizedKey)
                    .map(this::mapToOrderResponse)
                    .orElseGet(() -> createNewOrder(userId, request, normalizedKey));
        }

        return createNewOrder(userId, request, null);
    }

    @Transactional
    public OrderResponse createOrder(Long userId) {
        return createOrder(userId, null, null);
    }

    private OrderResponse createNewOrder(Long userId, OrderCreateRequest request, String idempotencyKey) {
        log.info("Creating new order for userId={}", userId);

        // Fetch cart
        List<CartItemResponse> cartItems =
                cartService.fetchItemsFromCart(String.valueOf(userId));

        if (cartItems == null || cartItems.isEmpty()) {
            log.warn("Order creation failed for userId={} because cart is empty", userId);
            throw new IllegalStateException("Cannot create order. Cart is empty.");
        }

        // Fetch user
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found with id: " + userId)
                );

        // Create Order
        Order order = new Order();
        order.setUser(user);
        order.setOrderStatus(OrderStatus.CREATED);
        order.setIdempotencyKey(idempotencyKey);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItemResponse cartItem : cartItems) {
            Integer quantity = cartItem.getQuantity();
            Long productId = cartItem.getProduct().getId();

            BigDecimal itemTotal = cartItem.getPrice()
                    .multiply(BigDecimal.valueOf(quantity));

            totalAmount = totalAmount.add(itemTotal);
            // Always attach a managed Product instance in this transaction.
            Product managedProduct = productRepository.findByIdAndActiveTrue(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
            int updated = productRepository.decrementStock(productId, quantity);
            if (updated == 0) {
                throw new InsufficientStockException("Insufficient stock for productId: " + productId);
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(managedProduct);
            orderItem.setQuantity(quantity);
            orderItem.setPrice(cartItem.getPrice());
            orderItem.setOrder(order);   // owning side

            orderItems.add(orderItem);
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request != null && request.getPromoCode() != null && !request.getPromoCode().isBlank()) {
            PromoApplyResponse promo = promoCodeService.applyPromo(request.getPromoCode(), totalAmount);
            discountAmount = promo.getDiscountAmount();
            order.setPromoCode(promo.getCode());
        }

        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(totalAmount.subtract(discountAmount));
        order.setOrderItems(orderItems);
        order.setShippingAddress(copyAddress(user.getAddress()));
        order.getStatusHistory().add(buildStatusHistory(order, order.getOrderStatus()));

        // Save
        Order savedOrder = orderRepository.save(order);
        log.info("Order persisted with id={} for userId={}", savedOrder.getId(), userId);
        notificationService.createNotification(
                userId,
                "Order placed successfully. Order ID: " + savedOrder.getId(),
                NotificationType.ORDER
        );

        // Clear cart (same transaction)
        cartItems.forEach(item ->
                cartService.deleteProductFromCart(
                        String.valueOf(userId),
                        item.getProduct().getId()
                )
        );

        // Fire-and-forget notification (async, non-blocking)
        sendOrderConfirmationAsync(savedOrder.getId(), user.getEmail());

        // Build Response
        return mapToResponse(savedOrder);
    }

    /**
     * Asynchronously sends an order confirmation notification.
     * This runs in a separate thread so the API response is not delayed.
     */
    @Async("notificationExecutor")
    public void sendOrderConfirmationAsync(Long orderId, String email) {
        log.info("Sending order confirmation for orderId={} to email={}", orderId, email);
        // TODO: integrate with email/notification service (e.g. SendGrid, SES)
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        log.info("Fetching all orders");
        return orderRepository.findAll().stream()
                .map(this::mapToOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(Long userId) {
        log.info("Fetching orders for userId={}", userId);
        return orderRepository.findByUserId(userId).stream()
                .map(this::mapToOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getOrdersByUser(Long userId, int page, int size) {
        log.info("Fetching paged orders for userId={}, page={}, size={}", userId, page, size);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return orderRepository.findByUserId(userId, pageable)
                .map(this::mapToSummaryResponse);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long orderId, Long requesterId, boolean isAdmin) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!isAdmin && !order.getUser().getId().equals(requesterId)) {
            throw new IllegalArgumentException("Order does not belong to the requesting user");
        }

        Payment payment = paymentRepository.findByOrder(order).orElse(null);
        return mapToDetailResponse(order, payment);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatusUpdateRequest request) {
        log.info("Updating status for orderId={} to {}", orderId, request.getStatus());
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        order.setOrderStatus(request.getStatus());
        orderStatusHistoryRepository.save(buildStatusHistory(order, request.getStatus()));
        Order saved = orderRepository.save(order);
        log.info("Order status updated for orderId={} to {}", orderId, saved.getOrderStatus());
        return mapToOrderResponse(saved);
    }

    private OrderResponse mapToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setItems(mapOrderItems(order.getOrderItems()));
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getOrderStatus());
        response.setCreatedAt(order.getCreatedAt());

        return response;
    }

    public OrderResponse mapToOrderResponse(Order order) {
        OrderResponse orderResponse = new  OrderResponse();
        orderResponse.setId(order.getId());
        orderResponse.setTotalAmount(order.getTotalAmount());
        orderResponse.setStatus(order.getOrderStatus());
        orderResponse.setCreatedAt(order.getCreatedAt());
        orderResponse.setItems(mapOrderItems(order.getOrderItems()));

        return orderResponse;
    }

    private OrderSummaryResponse mapToSummaryResponse(Order order) {
        OrderSummaryResponse response = new OrderSummaryResponse();
        response.setOrderId(order.getId());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getOrderStatus());
        response.setCreatedAt(order.getCreatedAt());
        return response;
    }

    private OrderDetailResponse mapToDetailResponse(Order order, Payment payment) {
        OrderDetailResponse response = new OrderDetailResponse();
        response.setOrderId(order.getId());
        response.setTotalAmount(order.getTotalAmount());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setPromoCode(order.getPromoCode());
        response.setStatus(order.getOrderStatus());
        response.setCreatedAt(order.getCreatedAt());
        response.setShippingAddress(mapAddress(order.getShippingAddress(), order.getUser().getAddress()));

        PaymentStatus paymentStatus = payment != null ? payment.getStatus() : PaymentStatus.PENDING;
        response.setPaymentStatus(paymentStatus);
        response.setItems(mapOrderItems(order.getOrderItems()));
        response.setOrderTimeline(mapOrderTimeline(order.getId()));
        response.setPaymentTimeline(mapPaymentTimeline(payment));
        return response;
    }

    private List<OrderItemDTO> mapOrderItems(List<OrderItem> items) {
        return items.stream()
                .map(item -> {
                    OrderItemDTO dto = new OrderItemDTO();
                    dto.setId(item.getId());
                    dto.setProductId(item.getProduct().getId());
                    dto.setProductName(item.getProduct().getName());
                    dto.setQuantity(item.getQuantity());
                    dto.setPrice(item.getPrice());
                    return dto;
                })
                .toList();
    }

    private List<OrderStatusTimelineEntry> mapOrderTimeline(Long orderId) {
        List<OrderStatusTimelineEntry> entries = orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(orderId)
                .stream()
                .map(history -> {
                    OrderStatusTimelineEntry entry = new OrderStatusTimelineEntry();
                    entry.setStatus(history.getStatus());
                    entry.setTimestamp(history.getCreatedAt());
                    return entry;
                })
                .toList();

        if (entries.isEmpty()) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
            OrderStatusTimelineEntry entry = new OrderStatusTimelineEntry();
            entry.setStatus(order.getOrderStatus());
            entry.setTimestamp(order.getCreatedAt());
            return List.of(entry);
        }

        return entries;
    }

    private List<PaymentStatusTimelineEntry> mapPaymentTimeline(Payment payment) {
        if (payment == null) {
            return List.of();
        }
        List<PaymentStatusTimelineEntry> entries = paymentStatusHistoryRepository.findByPaymentIdOrderByCreatedAtAsc(payment.getId())
                .stream()
                .map(history -> {
                    PaymentStatusTimelineEntry entry = new PaymentStatusTimelineEntry();
                    entry.setStatus(history.getStatus());
                    entry.setTimestamp(history.getCreatedAt());
                    return entry;
                })
                .toList();
        if (entries.isEmpty()) {
            PaymentStatusTimelineEntry entry = new PaymentStatusTimelineEntry();
            entry.setStatus(payment.getStatus());
            entry.setTimestamp(payment.getCreatedAt());
            return List.of(entry);
        }
        return entries;
    }

    private Address copyAddress(Address source) {
        if (source == null) {
            return null;
        }
        Address address = new Address();
        address.setStreet(source.getStreet());
        address.setCity(source.getCity());
        address.setState(source.getState());
        address.setZipcode(source.getZipcode());
        address.setCountry(source.getCountry());
        return address;
    }

    private AddressDTO mapAddress(Address shipping, Address fallback) {
        Address effective = shipping != null ? shipping : fallback;
        if (effective == null) {
            return null;
        }
        AddressDTO dto = new AddressDTO();
        dto.setStreet(effective.getStreet());
        dto.setCity(effective.getCity());
        dto.setState(effective.getState());
        dto.setZipcode(effective.getZipcode());
        dto.setCountry(effective.getCountry());
        return dto;
    }

    private OrderStatusHistory buildStatusHistory(Order order, OrderStatus status) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(status);
        return history;
    }
}
