package com.app.ecom.repository;

import com.app.ecom.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByActiveTrue();

    Optional<Product> findByIdAndActiveTrue(Long id);

    @Query("""
            SELECT p
            FROM com_products p
            WHERE p.active = true
              AND p.stockQuantity > 0
              AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    List<Product> searchProducts(@Param("keyword") String keyword);

    @Query("""
            SELECT p
            FROM com_products p
            WHERE p.active = true
              AND p.stockQuantity > 0
              AND (:keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Product> searchActiveProducts(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT p
            FROM com_products p
            WHERE p.active = true
              AND p.stockQuantity > 0
              AND p.category IN :categories
            """)
    Page<Product> findByCategoryIn(@Param("categories") List<String> categories, Pageable pageable);

    @Query("""
            SELECT p
            FROM com_products p
            WHERE p.active = true
              AND p.stockQuantity > 0
            """)
    Page<Product> findActiveProducts(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM com_products p
            WHERE p.id = :productId AND p.active = true
            """)
    Optional<Product> findByIdForUpdate(@Param("productId") Long productId);

}
