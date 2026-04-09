package com.app.ecom.controller;

import com.app.ecom.dto.PaymentRequest;
import com.app.ecom.dto.PaymentResponse;
import com.app.ecom.security.AppUserDetails;
import com.app.ecom.service.PaymentService;
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

@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> initiatePayment(
            @AuthenticationPrincipal AppUserDetails currentUser,
            @Valid @RequestBody PaymentRequest request) {
        log.info("Initiate payment request received for userId={}, orderId={}", currentUser.getId(), request.getOrderId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.initiatePayment(currentUser.getId(), request));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrder(
            @AuthenticationPrincipal AppUserDetails currentUser,
            @PathVariable @Positive(message = "orderId must be positive") Long orderId) {
        log.info("Get payment by orderId={} for userId={}", orderId, currentUser.getId());
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId, currentUser.getId()));
    }

    /**
     * Webhook endpoint for payment gateway callbacks.
     * In production, this should be secured with a gateway-specific signature verification.
     */
    @PostMapping("/webhook")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> handleGatewayCallback(
            @RequestParam String transactionId,
            @RequestParam String status) {
        log.info("Gateway callback received for transactionId={}, status={}", transactionId, status);
        return ResponseEntity.ok(paymentService.handleGatewayCallback(transactionId, status));
    }
}
