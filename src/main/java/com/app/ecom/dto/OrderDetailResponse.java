package com.app.ecom.dto;

import com.app.ecom.model.OrderStatus;
import com.app.ecom.model.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDetailResponse {
    private Long orderId;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private String promoCode;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;
    private AddressDTO shippingAddress;
    private List<OrderItemDTO> items;
    private List<OrderStatusTimelineEntry> orderTimeline;
    private List<PaymentStatusTimelineEntry> paymentTimeline;
}
