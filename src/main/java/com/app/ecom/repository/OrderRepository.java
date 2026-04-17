package com.app.ecom.repository;

import com.app.ecom.model.Order;
import com.app.ecom.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM com_orders o WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
    List<Order> findByUserId(@Param("userId") Long userId);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    Page<Order> findByOrderStatus(OrderStatus status, Pageable pageable);

    @Query("""
            SELECT DISTINCT o
            FROM com_orders o
            LEFT JOIN FETCH o.orderItems oi
            LEFT JOIN FETCH oi.product
            WHERE o.id = :orderId
            """)
    Optional<Order> findByIdWithItems(@Param("orderId") Long orderId);

    @Query("""
            SELECT COUNT(oi) > 0
            FROM com_orders o
            JOIN o.orderItems oi
            WHERE o.user.id = :userId AND oi.product.id = :productId
            """)
    boolean hasPurchasedProduct(@Param("userId") Long userId, @Param("productId") Long productId);

    Optional<Order> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    @Query("""
            SELECT DISTINCT p.category
            FROM com_orders o
            JOIN o.orderItems oi
            JOIN oi.product p
            WHERE o.user.id = :userId AND p.category IS NOT NULL
            """)
    List<String> findDistinctCategoriesByUserId(@Param("userId") Long userId);
}
