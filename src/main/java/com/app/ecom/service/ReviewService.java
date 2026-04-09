package com.app.ecom.service;

import com.app.ecom.dto.ReviewRequest;
import com.app.ecom.dto.ReviewResponse;
import com.app.ecom.exception.ResourceNotFoundException;
import com.app.ecom.model.Product;
import com.app.ecom.model.Review;
import com.app.ecom.model.User;
import com.app.ecom.repository.ProductRepository;
import com.app.ecom.repository.ReviewRepository;
import com.app.ecom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReviewResponse addReview(Long userId, ReviewRequest request) {
        log.info("Adding review for userId={}, productId={}", userId, request.getProductId());

        Product product = productRepository.findByIdAndActiveTrue(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        if (reviewRepository.existsByProductIdAndUserId(request.getProductId(), userId)) {
            throw new IllegalStateException("You have already reviewed this product");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review saved = reviewRepository.save(review);
        log.info("Review created with id={} for productId={}", saved.getId(), request.getProductId());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByProduct(Long productId, int page, int size) {
        log.info("Fetching reviews for productId={}, page={}, size={}", productId, page, size);
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return reviewRepository.findByProductId(productId, pageable)
                .map(this::mapToResponse);
    }

    public Double getAverageRating(Long productId) {
        return reviewRepository.findAverageRatingByProductId(productId);
    }

    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        if (!review.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You can only delete your own reviews");
        }

        reviewRepository.delete(review);
        log.info("Review deleted with id={} by userId={}", reviewId, userId);
    }

    private ReviewResponse mapToResponse(Review review) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setProductId(review.getProduct().getId());
        response.setUserId(review.getUser().getId());
        response.setUserFullName(review.getUser().getFirstName() + " " + review.getUser().getLastName());
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());
        return response;
    }
}
