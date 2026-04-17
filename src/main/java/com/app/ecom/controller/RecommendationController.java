package com.app.ecom.controller;

import com.app.ecom.dto.ProductResponse;
import com.app.ecom.security.AppUserDetails;
import com.app.ecom.service.RecommendationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getRecommendations(
            @AuthenticationPrincipal AppUserDetails currentUser,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "limit must be at least 1")
            @Max(value = 50, message = "limit must be at most 50") int limit) {
        log.info("Fetch recommendations request received for userId={}, limit={}", currentUser.getId(), limit);
        return ResponseEntity.ok(recommendationService.getRecommendations(currentUser.getId(), limit));
    }
}
