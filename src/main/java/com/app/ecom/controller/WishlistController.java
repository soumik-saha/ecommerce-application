package com.app.ecom.controller;

import com.app.ecom.dto.WishlistRequest;
import com.app.ecom.dto.WishlistResponse;
import com.app.ecom.security.AppUserDetails;
import com.app.ecom.service.WishlistService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping
    public ResponseEntity<WishlistResponse> addToWishlist(
            @AuthenticationPrincipal AppUserDetails currentUser,
            @Valid @RequestBody WishlistRequest request) {
        log.info("Add to wishlist request received for userId={}, productId={}", currentUser.getId(), request.getProductId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(wishlistService.addToWishlist(currentUser.getId(), request));
    }

    @GetMapping
    public ResponseEntity<List<WishlistResponse>> getWishlist(
            @AuthenticationPrincipal AppUserDetails currentUser) {
        log.info("Fetch wishlist request received for userId={}", currentUser.getId());
        return ResponseEntity.ok(wishlistService.getWishlist(currentUser.getId()));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeFromWishlist(
            @AuthenticationPrincipal AppUserDetails currentUser,
            @PathVariable @Positive(message = "productId must be positive") Long productId) {
        log.info("Remove wishlist item request received for userId={}, productId={}", currentUser.getId(), productId);
        wishlistService.removeFromWishlist(currentUser.getId(), productId);
        return ResponseEntity.noContent().build();
    }
}
