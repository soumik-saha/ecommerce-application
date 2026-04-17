package com.app.ecom.repository;

import com.app.ecom.model.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    boolean existsByOrderId(Long orderId);

    List<ReturnRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
}
