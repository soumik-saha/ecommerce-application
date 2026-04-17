package com.app.ecom.service;

import com.app.ecom.dto.WishlistRequest;
import com.app.ecom.dto.WishlistResponse;
import com.app.ecom.exception.ResourceNotFoundException;
import com.app.ecom.model.Product;
import com.app.ecom.model.User;
import com.app.ecom.model.Wishlist;
import com.app.ecom.repository.ProductRepository;
import com.app.ecom.repository.UserRepository;
import com.app.ecom.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public WishlistResponse addToWishlist(Long userId, WishlistRequest request) {
        if (wishlistRepository.existsByUserIdAndProductId(userId, request.getProductId())) {
            throw new IllegalStateException("Product already in wishlist");
        }

        Product product = productRepository.findByIdAndActiveTrue(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        wishlist.setProduct(product);

        Wishlist saved = wishlistRepository.save(wishlist);
        log.info("Wishlist created with id={} for userId={}, productId={}", saved.getId(), userId, product.getId());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<WishlistResponse> getWishlist(Long userId) {
        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void removeFromWishlist(Long userId, Long productId) {
        Wishlist wishlist = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found"));
        wishlistRepository.delete(wishlist);
        log.info("Wishlist item removed for userId={}, productId={}", userId, productId);
    }

    private WishlistResponse mapToResponse(Wishlist wishlist) {
        WishlistResponse response = new WishlistResponse();
        response.setId(wishlist.getId());
        response.setProductId(wishlist.getProduct().getId());
        response.setProductName(wishlist.getProduct().getName());
        response.setPrice(wishlist.getProduct().getPrice());
        response.setImageUrl(wishlist.getProduct().getImageUrl());
        response.setCreatedAt(wishlist.getCreatedAt());
        return response;
    }
}
