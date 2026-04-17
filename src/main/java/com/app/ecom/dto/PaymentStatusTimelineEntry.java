package com.app.ecom.dto;

import com.app.ecom.model.PaymentStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentStatusTimelineEntry {
    private PaymentStatus status;
    private LocalDateTime timestamp;
}
