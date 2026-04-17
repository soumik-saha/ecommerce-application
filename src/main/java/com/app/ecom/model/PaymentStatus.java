package com.app.ecom.model;

public enum PaymentStatus {
    PENDING,
    // Legacy status; prefer PENDING for newly created payments.
    INITIATED,
    SUCCESS,
    FAILED,
    REFUNDED,
    CANCELLED
}
