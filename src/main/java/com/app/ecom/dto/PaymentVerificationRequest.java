package com.app.ecom.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentVerificationRequest {

    @NotBlank(message = "Gateway transaction ID is required")
    private String gatewayTransactionId;

    @NotBlank(message = "Status is required")
    private String status;
}
