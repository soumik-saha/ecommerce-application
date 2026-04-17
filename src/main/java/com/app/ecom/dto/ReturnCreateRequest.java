package com.app.ecom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReturnCreateRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotBlank(message = "Reason is required")
    private String reason;
}
