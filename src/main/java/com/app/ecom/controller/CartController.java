package com.app.ecom.controller;

import com.app.ecom.dto.CartItemRequest;
import com.app.ecom.dto.CartItemResponse;
import com.app.ecom.security.AppUserDetails;
import com.app.ecom.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;

    @PostMapping
    public ResponseEntity<String> addToCart(
            @AuthenticationPrincipal AppUserDetails currentUser,
            @Valid @RequestBody CartItemRequest cartItemRequest) {
        String userId = String.valueOf(currentUser.getId());
        log.info("Add to cart request received for userId={}, productId={}, quantity={}", userId, cartItemRequest.getProductId(), cartItemRequest.getQuantity());
        if(!cartService.addToCart(userId, cartItemRequest)) {
            log.warn("Add to cart failed for userId={}, productId={}", userId, cartItemRequest.getProductId());
            return ResponseEntity.badRequest().body("Product out of stock or User not found or Product not found");
        }
        log.info("Product added to cart for userId={}, productId={}", userId, cartItemRequest.getProductId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> deleteFromCart(
            @AuthenticationPrincipal AppUserDetails currentUser,
            @PathVariable("productId") Long productId) {
        String userId = String.valueOf(currentUser.getId());
        log.info("Delete from cart request received for userId={}, productId={}", userId, productId);
        Boolean deleted = cartService.deleteProductFromCart(userId, productId);
        if (!deleted) {
            log.warn("Delete from cart failed for userId={}, productId={}", userId, productId);
        }
        return deleted ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<CartItemResponse>> getItemsFromCart(
            @AuthenticationPrincipal AppUserDetails currentUser) {
        String userId = String.valueOf(currentUser.getId());
        log.info("Fetch cart items request received for userId={}", userId);
        return new ResponseEntity<>(cartService.fetchItemsFromCart(userId), HttpStatus.OK);
    }
}
