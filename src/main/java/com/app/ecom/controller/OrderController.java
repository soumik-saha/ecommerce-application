package com.app.ecom.controller;

import com.app.ecom.dto.OrderResponse;
import com.app.ecom.security.AppUserDetails;
import com.app.ecom.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
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

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        log.info("Fetch all orders request received");
        List<OrderResponse> orders = orderService.getAllOrders();
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }
}
