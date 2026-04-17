package com.app.ecom.dto;

import com.app.ecom.model.DiscountType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PromoApplyResponse {
    private String code;
    private DiscountType discountType;
    private BigDecimal value;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
}
