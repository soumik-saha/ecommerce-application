package com.app.ecom.dto;

import com.app.ecom.model.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private String currency;
    private String provider;
    private PaymentStatus status;
    private String gatewayTransactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
