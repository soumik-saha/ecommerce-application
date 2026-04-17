package com.app.ecom.service;

import com.app.ecom.dto.PromoApplyResponse;
import com.app.ecom.exception.ResourceNotFoundException;
import com.app.ecom.model.DiscountType;
import com.app.ecom.model.PromoCode;
import com.app.ecom.repository.PromoCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;

    @Transactional
    public PromoApplyResponse applyPromo(String code, BigDecimal orderAmount) {
        PromoCode promoCode = getValidPromoCode(code);
        return buildResponse(promoCode, orderAmount);
    }

    @Transactional
    public PromoApplyResponse consumePromo(String code, BigDecimal orderAmount) {
        PromoCode promoCode = getValidPromoCodeForUpdate(code);
        PromoApplyResponse response = buildResponse(promoCode, orderAmount);

        promoCode.setUsageCount(promoCode.getUsageCount() + 1);
        promoCodeRepository.save(promoCode);
        log.info("Promo code {} consumed. Usage {}/{}", promoCode.getCode(), promoCode.getUsageCount(), promoCode.getUsageLimit());
        return response;
    }

    private PromoCode getValidPromoCode(String code) {
        String normalized = normalize(code);
        PromoCode promoCode = promoCodeRepository.findById(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Promo code not found: " + normalized));

        if (promoCode.getExpiryDate() != null && promoCode.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Promo code has expired");
        }

        if (promoCode.getUsageLimit() != null && promoCode.getUsageLimit() > 0
                && promoCode.getUsageCount() >= promoCode.getUsageLimit()) {
            throw new IllegalStateException("Promo code usage limit reached");
        }
        return promoCode;
    }

    private PromoCode getValidPromoCodeForUpdate(String code) {
        String normalized = normalize(code);
        PromoCode promoCode = promoCodeRepository.findByCodeForUpdate(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Promo code not found: " + normalized));

        if (promoCode.getExpiryDate() != null && promoCode.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Promo code has expired");
        }

        if (promoCode.getUsageLimit() != null && promoCode.getUsageLimit() > 0
                && promoCode.getUsageCount() >= promoCode.getUsageLimit()) {
            throw new IllegalStateException("Promo code usage limit reached");
        }
        return promoCode;
    }

    private PromoApplyResponse buildResponse(PromoCode promoCode, BigDecimal orderAmount) {
        BigDecimal discountAmount;
        if (promoCode.getDiscountType() == DiscountType.PERCENT) {
            discountAmount = orderAmount
                    .multiply(promoCode.getValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discountAmount = promoCode.getValue();
        }

        if (discountAmount.compareTo(orderAmount) > 0) {
            discountAmount = orderAmount;
        }

        PromoApplyResponse response = new PromoApplyResponse();
        response.setCode(promoCode.getCode());
        response.setDiscountType(promoCode.getDiscountType());
        response.setValue(promoCode.getValue());
        response.setDiscountAmount(discountAmount);
        response.setFinalAmount(orderAmount.subtract(discountAmount));
        return response;
    }

    private String normalize(String code) {
        if (code == null) {
            return null;
        }
        return code.trim().toUpperCase();
    }
}
