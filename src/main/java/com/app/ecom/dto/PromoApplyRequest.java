package com.app.ecom.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PromoApplyRequest {

    @NotBlank(message = "Promo code is required")
    private String code;

    @NotNull(message = "Order amount is required")
    @DecimalMin(value = "0.01", message = "Order amount must be greater than zero")
    private BigDecimal orderAmount;
}
