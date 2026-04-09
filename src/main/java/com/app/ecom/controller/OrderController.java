package com.app.ecom.controller;

import com.app.ecom.dto.OrderResponse;
import com.app.ecom.dto.OrderStatusUpdateRequest;
import com.app.ecom.security.AppUserDetails;
import com.app.ecom.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public ResponseEntity<OrderResponse> createOrder(@AuthenticationPrincipal AppUserDetails currentUser) {
        log.info("Create order request received for userId={}", currentUser.getId());
        OrderResponse order = orderService.createOrder(currentUser.getId());
        log.info("Order created successfully with orderId={} for userId={}", order.getId(), currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/my")
    public ResponseEntity<List<OrderResponse>> getMyOrders(@AuthenticationPrincipal AppUserDetails currentUser) {
        log.info("Fetch my orders request received for userId={}", currentUser.getId());
        return ResponseEntity.ok(orderService.getOrdersByUser(currentUser.getId()));
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
