package com.app.ecom.controller;

import com.app.ecom.dto.OrderCreateRequest;
import com.app.ecom.dto.OrderDetailResponse;
import com.app.ecom.dto.OrderResponse;
import com.app.ecom.dto.OrderStatusUpdateRequest;
import com.app.ecom.dto.OrderSummaryResponse;
import com.app.ecom.model.UserRole;
import com.app.ecom.security.AppUserDetails;
import com.app.ecom.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal AppUserDetails currentUser,
            @Valid @RequestBody(required = false) OrderCreateRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        log.info("Create order request received for userId={}", currentUser.getId());
        OrderResponse order = orderService.createOrder(currentUser.getId(), request, idempotencyKey);
        log.info("Order created successfully with orderId={} for userId={}", order.getId(), currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping(params = {"page", "limit"})
    public ResponseEntity<Page<OrderSummaryResponse>> getMyOrdersPaged(
            @AuthenticationPrincipal AppUserDetails currentUser,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or greater") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "limit must be at least 1")
            @Max(value = 50, message = "limit must be at most 50") int limit) {
        log.info("Fetch paged orders request received for userId={}, page={}, limit={}", currentUser.getId(), page, limit);
        return ResponseEntity.ok(orderService.getOrdersByUser(currentUser.getId(), page, limit));
    }

    @GetMapping("/my")
    public ResponseEntity<List<OrderResponse>> getMyOrders(@AuthenticationPrincipal AppUserDetails currentUser) {
        log.info("Fetch my orders request received for userId={}", currentUser.getId());
        return ResponseEntity.ok(orderService.getOrdersByUser(currentUser.getId()));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrderDetail(
            @AuthenticationPrincipal AppUserDetails currentUser,
            @PathVariable @Positive(message = "orderId must be positive") Long orderId) {
        log.info("Fetch order detail request received for orderId={} userId={}", orderId, currentUser.getId());
        boolean isAdmin = UserRole.ADMIN.name().equalsIgnoreCase(currentUser.getRole());
        return ResponseEntity.ok(orderService.getOrderDetail(orderId, currentUser.getId(), isAdmin));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        log.info("Fetch all orders request received");
        List<OrderResponse> orders = orderService.getAllOrders();
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable @Positive(message = "orderId must be positive") Long orderId,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        log.info("Update order status request received for orderId={} to {}", orderId, request.getStatus());
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, request));
    }
}
