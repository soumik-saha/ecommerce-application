package com.app.ecom.dto;

import com.app.ecom.model.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private OrderStatus status;
}
