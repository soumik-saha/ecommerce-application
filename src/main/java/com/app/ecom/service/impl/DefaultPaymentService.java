package com.app.ecom.service.impl;

import com.app.ecom.dto.PaymentRequest;
import com.app.ecom.dto.PaymentResponse;
import com.app.ecom.exception.ResourceNotFoundException;
import com.app.ecom.model.Order;
import com.app.ecom.model.OrderStatus;
import com.app.ecom.model.Payment;
import com.app.ecom.model.PaymentStatus;
import com.app.ecom.model.User;
import com.app.ecom.repository.OrderRepository;
import com.app.ecom.repository.PaymentRepository;
import com.app.ecom.repository.UserRepository;
import com.app.ecom.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultPaymentService implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public PaymentResponse initiatePayment(Long userId, PaymentRequest request) {
        log.info("Initiating payment for userId={}, orderId={}", userId, request.getOrderId());

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + request.getOrderId()));

        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to the requesting user");
        }

        if (paymentRepository.findByOrder(order).isPresent()) {
            throw new IllegalStateException("Payment already initiated for orderId: " + request.getOrderId());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setUser(user);
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setProvider(request.getProvider());
        payment.setGatewayTransactionId(request.getGatewayTransactionId());
        payment.setStatus(PaymentStatus.INITIATED);

        // Move order to PAYMENT_PENDING so it cannot be modified
        order.setOrderStatus(OrderStatus.PAYMENT_PENDING);
        orderRepository.save(order);

        Payment saved = paymentRepository.save(payment);
        log.info("Payment created with id={} for orderId={}", saved.getId(), order.getId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId, Long requestingUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getUser().getId().equals(requestingUserId)) {
            throw new IllegalArgumentException("Order does not belong to the requesting user");
        }

        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new ResourceNotFoundException("No payment found for orderId: " + orderId));

        return mapToResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse handleGatewayCallback(String gatewayTransactionId, String newStatus) {
        log.info("Gateway callback received for transactionId={}, status={}", gatewayTransactionId, newStatus);

        Payment payment = paymentRepository.findByGatewayTransactionId(gatewayTransactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found for transactionId: " + gatewayTransactionId));

        PaymentStatus resolvedStatus;
        try {
            resolvedStatus = PaymentStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown payment status: " + newStatus);
        }

        payment.setStatus(resolvedStatus);

        // Sync order status based on payment outcome
        Order order = payment.getOrder();
        if (resolvedStatus == PaymentStatus.SUCCESS) {
            order.setOrderStatus(OrderStatus.PAYMENT_COMPLETED);
        } else if (resolvedStatus == PaymentStatus.FAILED) {
            order.setOrderStatus(OrderStatus.PAYMENT_FAILED);
        } else if (resolvedStatus == PaymentStatus.REFUNDED) {
            order.setOrderStatus(OrderStatus.REFUNDED);
        }
        orderRepository.save(order);

        Payment saved = paymentRepository.save(payment);
        log.info("Payment updated to status={} for transactionId={}", resolvedStatus, gatewayTransactionId);
        return mapToResponse(saved);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setOrderId(payment.getOrder().getId());
        response.setUserId(payment.getUser().getId());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setProvider(payment.getProvider());
        response.setStatus(payment.getStatus());
        response.setGatewayTransactionId(payment.getGatewayTransactionId());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        return response;
    }
}
