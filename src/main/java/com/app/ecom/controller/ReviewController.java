package com.app.ecom.controller;

import com.app.ecom.dto.ReviewRequest;
import com.app.ecom.dto.ReviewResponse;
import com.app.ecom.security.AppUserDetails;
import com.app.ecom.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> addReview(
            @AuthenticationPrincipal AppUserDetails currentUser,
            @Valid @RequestBody ReviewRequest request) {
        log.info("Add review request received for userId={}, productId={}", currentUser.getId(), request.getProductId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.addReview(currentUser.getId(), request));
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByProduct(
            @PathVariable @Positive(message = "productId must be positive") Long productId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or greater") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be at least 1")
            @Max(value = 50, message = "size must be at most 50") int size) {
        log.info("Fetching reviews for productId={}, page={}, size={}", productId, page, size);
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId, page, size));
    }

    @GetMapping("/products/{productId}/rating")
    public ResponseEntity<Map<String, Object>> getAverageRating(
            @PathVariable @Positive(message = "productId must be positive") Long productId) {
        log.info("Fetching average rating for productId={}", productId);
        Double avg = reviewService.getAverageRating(productId);
        return ResponseEntity.ok(Map.of(
                "productId", productId,
                "averageRating", avg != null ? avg : 0.0
        ));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @AuthenticationPrincipal AppUserDetails currentUser,
            @PathVariable @Positive(message = "reviewId must be positive") Long reviewId) {
        log.info("Delete review request received for reviewId={} by userId={}", reviewId, currentUser.getId());
        reviewService.deleteReview(reviewId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
