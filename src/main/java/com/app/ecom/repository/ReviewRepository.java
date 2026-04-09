package com.app.ecom.repository;

import com.app.ecom.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByProductId(Long productId, Pageable pageable);

    boolean existsByProductIdAndUserId(Long productId, Long userId);

    Optional<Review> findByProductIdAndUserId(Long productId, Long userId);

    @Query("SELECT AVG(r.rating) FROM com_reviews r WHERE r.product.id = :productId")
    Double findAverageRatingByProductId(@Param("productId") Long productId);
}
