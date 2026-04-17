package com.app.ecom.service.impl;

import com.app.ecom.dto.PaymentRequest;
import com.app.ecom.dto.PaymentResponse;
import com.app.ecom.exception.ResourceNotFoundException;
import com.app.ecom.model.Order;
import com.app.ecom.model.OrderStatus;
import com.app.ecom.model.NotificationType;
import com.app.ecom.model.OrderStatusHistory;
import com.app.ecom.model.Payment;
import com.app.ecom.model.PaymentStatus;
import com.app.ecom.model.PaymentStatusHistory;
import com.app.ecom.model.User;
import com.app.ecom.repository.OrderRepository;
import com.app.ecom.repository.OrderStatusHistoryRepository;
import com.app.ecom.repository.PaymentRepository;
import com.app.ecom.repository.PaymentStatusHistoryRepository;
import com.app.ecom.repository.UserRepository;
import com.app.ecom.service.NotificationService;
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
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final PaymentStatusHistoryRepository paymentStatusHistoryRepository;
    private final NotificationService notificationService;

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

        if (order.getTotalAmount() == null || request.getAmount() == null
                || order.getTotalAmount().compareTo(request.getAmount()) != 0) {
            throw new IllegalArgumentException("Payment amount does not match order total");
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
        payment.setStatus(PaymentStatus.PENDING);

        // Move order to PAYMENT_PENDING so it cannot be modified
        order.setOrderStatus(OrderStatus.PAYMENT_PENDING);
        orderRepository.save(order);
        orderStatusHistoryRepository.save(buildOrderStatusHistory(order, OrderStatus.PAYMENT_PENDING));

        Payment saved = paymentRepository.save(payment);
        paymentStatusHistoryRepository.save(buildPaymentStatusHistory(saved, saved.getStatus()));
        log.info("Payment created with id={} for orderId={}", saved.getId(), order.getId());
        notificationService.createNotification(
                userId,
                "Payment initiated for order " + order.getId(),
                NotificationType.PAYMENT
        );
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
        paymentStatusHistoryRepository.save(buildPaymentStatusHistory(payment, resolvedStatus));

        // Sync order status based on payment outcome
        Order order = payment.getOrder();
        if (resolvedStatus == PaymentStatus.SUCCESS) {
            order.setOrderStatus(OrderStatus.PAYMENT_COMPLETED);
            orderStatusHistoryRepository.save(buildOrderStatusHistory(order, OrderStatus.PAYMENT_COMPLETED));
        } else if (resolvedStatus == PaymentStatus.FAILED) {
            order.setOrderStatus(OrderStatus.PAYMENT_FAILED);
            orderStatusHistoryRepository.save(buildOrderStatusHistory(order, OrderStatus.PAYMENT_FAILED));
        } else if (resolvedStatus == PaymentStatus.REFUNDED) {
            order.setOrderStatus(OrderStatus.REFUNDED);
            orderStatusHistoryRepository.save(buildOrderStatusHistory(order, OrderStatus.REFUNDED));
        }
        orderRepository.save(order);

        Payment saved = paymentRepository.save(payment);
        log.info("Payment updated to status={} for transactionId={}", resolvedStatus, gatewayTransactionId);
        notificationService.createNotification(
                order.getUser().getId(),
                "Payment status updated to " + resolvedStatus + " for order " + order.getId(),
                NotificationType.PAYMENT
        );
        return mapToResponse(saved);
    }

    private OrderStatusHistory buildOrderStatusHistory(Order order, OrderStatus status) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(status);
        return history;
    }

    private PaymentStatusHistory buildPaymentStatusHistory(Payment payment, PaymentStatus status) {
        PaymentStatusHistory history = new PaymentStatusHistory();
        history.setPayment(payment);
        history.setStatus(status);
        return history;
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
