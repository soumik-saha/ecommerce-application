package com.app.ecom.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(name = "com_promo_codes")
@Table(name = "com_promo_codes")
@Data
@NoArgsConstructor
public class PromoCode {

    @Id
    @Column(length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DiscountType discountType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private Integer usageLimit = 0;

    @Column(nullable = false)
    private Integer usageCount = 0;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
