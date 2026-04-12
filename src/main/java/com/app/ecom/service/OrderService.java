package com.app.ecom.service;

import com.app.ecom.dto.CartItemResponse;
import com.app.ecom.dto.OrderItemDTO;
import com.app.ecom.dto.OrderResponse;
import com.app.ecom.dto.OrderStatusUpdateRequest;
import com.app.ecom.exception.ResourceNotFoundException;
import com.app.ecom.model.Order;
import com.app.ecom.model.OrderItem;
import com.app.ecom.model.OrderStatus;
import com.app.ecom.model.Product;
import com.app.ecom.model.User;
import com.app.ecom.repository.OrderRepository;
import com.app.ecom.repository.ProductRepository;
import com.app.ecom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Transactional
    public OrderResponse createOrder(Long userId) {
        log.info("Creating order for userId={}", userId);

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
        order.setOrderStatus(OrderStatus.CONFIRMED);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItemResponse cartItem : cartItems) {

            BigDecimal itemTotal =
                    cartItem.getPrice()
                            .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            totalAmount = totalAmount.add(itemTotal);

            Long productId = cartItem.getProduct().getId();
            // Always attach a managed Product instance in this transaction.
            Product managedProduct = productRepository.findByIdAndActiveTrue(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(managedProduct);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getPrice());
            orderItem.setOrder(order);   // owning side

            orderItems.add(orderItem);
        }

        order.setTotalAmount(totalAmount);
        order.setOrderItems(orderItems);

        // Save
        Order savedOrder = orderRepository.save(order);
        log.info("Order persisted with id={} for userId={}", savedOrder.getId(), userId);

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

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatusUpdateRequest request) {
        log.info("Updating status for orderId={} to {}", orderId, request.getStatus());
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        order.setOrderStatus(request.getStatus());
        Order saved = orderRepository.save(order);
        log.info("Order status updated for orderId={} to {}", orderId, saved.getOrderStatus());
        return mapToOrderResponse(saved);
    }

    private OrderResponse mapToResponse(Order order) {

        List<OrderItemDTO> itemDTOList = order.getOrderItems()
                .stream()
                .map(item -> {
                    OrderItemDTO dto = new OrderItemDTO();
                    dto.setId(item.getId());
                    dto.setProductId(item.getProduct().getId());
                    dto.setQuantity(item.getQuantity());
                    dto.setPrice(item.getPrice());
                    return dto;
                })
                .toList();

        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setItems(itemDTOList);
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getOrderStatus());

        return response;
    }

    public OrderResponse mapToOrderResponse(Order order) {
        OrderResponse orderResponse = new  OrderResponse();
        orderResponse.setId(order.getId());
        orderResponse.setTotalAmount(order.getTotalAmount());
        orderResponse.setStatus(order.getOrderStatus());
        List<OrderItemDTO> orderItemList = new ArrayList<>();

        for(OrderItem orderItem:order.getOrderItems()) {
            OrderItemDTO orderItemDTO = new OrderItemDTO();
            orderItemDTO.setId(orderItem.getId());
            orderItemDTO.setProductId(orderItem.getProduct().getId());
            orderItemDTO.setQuantity(orderItem.getQuantity());
            orderItemDTO.setPrice(orderItem.getPrice());
            orderItemList.add(orderItemDTO);
        }

        orderResponse.setItems(orderItemList);

        return orderResponse;
    }
}
