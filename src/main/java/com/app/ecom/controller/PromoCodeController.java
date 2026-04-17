package com.app.ecom.controller;

import com.app.ecom.dto.PromoApplyRequest;
import com.app.ecom.dto.PromoApplyResponse;
import com.app.ecom.service.PromoCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/promo")
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    @PostMapping("/apply")
    public ResponseEntity<PromoApplyResponse> applyPromo(@Valid @RequestBody PromoApplyRequest request) {
        log.info("Promo apply request received for code={}", request.getCode());
        return ResponseEntity.ok(promoCodeService.applyPromo(request.getCode(), request.getOrderAmount()));
    }
}
