package com.app.ecom.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotBlank(message = "Provider is required")
    private String provider;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    /** Optional: gateway transaction reference supplied by the frontend. */
    private String gatewayTransactionId;
}
