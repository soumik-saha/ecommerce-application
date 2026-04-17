package com.app.ecom.dto;

import com.app.ecom.model.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderStatusTimelineEntry {
    private OrderStatus status;
    private LocalDateTime timestamp;
}
