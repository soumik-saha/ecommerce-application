package com.app.ecom.service;

import com.app.ecom.dto.PaymentRequest;
import com.app.ecom.dto.PaymentResponse;

public interface PaymentService {

    /**
     * Initiate a payment for an order.
     * Implementations may delegate to Stripe, Razorpay, COD, etc.
     */
    PaymentResponse initiatePayment(Long userId, PaymentRequest request);

    /**
     * Retrieve payment details for an order.
     */
    PaymentResponse getPaymentByOrderId(Long orderId, Long requestingUserId);

    /**
     * Handle gateway callback/webhook to update payment status.
     */
    PaymentResponse handleGatewayCallback(String gatewayTransactionId, String newStatus);
}
