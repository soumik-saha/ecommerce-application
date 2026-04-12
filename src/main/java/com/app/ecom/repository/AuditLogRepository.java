package com.app.ecom.repository;

import com.app.ecom.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUserId(Long userId);

    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

    List<AuditLog> findByAction(String action);

    Optional<AuditLog> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    Optional<AuditLog> findByIdempotencyKey(String idempotencyKey);

    Page<AuditLog> findByCreatedAtBetween(Instant startDate, Instant endDate, Pageable pageable);

    Page<AuditLog> findByUserIdAndCreatedAtBetween(Long userId, Instant startDate, Instant endDate, Pageable pageable);
}

