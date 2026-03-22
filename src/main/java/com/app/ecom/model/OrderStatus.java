package com.app.ecom.model;

public enum OrderStatus {

    // Order is pending
    PENDING,

    // Order created but not processed yet
    CREATED,

    // Awaiting payment from user
    PAYMENT_PENDING,

    // Payment completed successfully
    PAYMENT_COMPLETED,

    // Payment failed
    PAYMENT_FAILED,

    // Order confirmed by system
    CONFIRMED,

    // Stock being prepared / packed
    PROCESSING,

    // Packed and ready for dispatch
    PACKED,

    // Handed over to courier
    SHIPPED,

    // Out for delivery
    OUT_FOR_DELIVERY,

    // Delivered successfully
    DELIVERED,

    // Customer cancelled before shipment
    CANCELLED,

    // Cancelled automatically (timeout / payment failure)
    AUTO_CANCELLED,

    // Delivery attempt failed
    DELIVERY_FAILED,

    // Returned by customer
    RETURN_REQUESTED,

    // Return approved
    RETURN_APPROVED,

    // Return picked up
    RETURN_PICKED_UP,

    // Refund initiated
    REFUND_INITIATED,

    // Refund completed
    REFUNDED,

    // Order closed after completion
    COMPLETED
}