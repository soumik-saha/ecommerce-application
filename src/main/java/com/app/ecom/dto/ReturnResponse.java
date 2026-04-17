package com.app.ecom.dto;

import com.app.ecom.model.ReturnStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReturnResponse {
    private Long id;
    private Long orderId;
    private String reason;
    private ReturnStatus status;
    private LocalDateTime createdAt;
}
