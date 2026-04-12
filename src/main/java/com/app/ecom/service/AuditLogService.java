package com.app.ecom.service;

import com.app.ecom.dto.AuditLogRequest;
import com.app.ecom.model.AuditLog;
import com.app.ecom.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    @Transactional
    public AuditLog logAudit(Long userId, String entityType, Long entityId, String action, String description) {
        return logAudit(userId, entityType, entityId, action, null, null, description);
    }

    @Transactional
    public AuditLog logAudit(Long userId, String entityType, Long entityId, String action,
                              String oldValue, String newValue, String description) {
        return logAudit(userId, entityType, entityId, action, oldValue, newValue, description, null);
    }

    @Transactional
    public AuditLog logAudit(Long userId, String entityType, Long entityId, String action,
                             String oldValue, String newValue, String description, String idempotencyKey) {
        String ipAddress = getClientIp();
        String userAgent = getUserAgent();

        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .oldValue(oldValue)
                .newValue(newValue)
                .description(description)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .idempotencyKey(idempotencyKey)
                .build();

        AuditLog saved = auditLogRepository.save(auditLog);
        log.info("Audit log created: user={}, entity={}:{}, action={}", userId, entityType, entityId, action);
        return saved;
    }

    @Transactional
    public boolean logAuditIfAbsent(Long userId, String entityType, Long entityId, String action,
                                    String oldValue, String newValue, String description, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            boolean exists = userId != null
                    ? auditLogRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey).isPresent()
                    : auditLogRepository.findByIdempotencyKey(idempotencyKey).isPresent();
            if (exists) {
                log.info("Skipping duplicate audit log for userId={} idempotencyKey={}", userId, idempotencyKey);
                return false;
            }
        }

        logAudit(userId, entityType, entityId, action, oldValue, newValue, description, idempotencyKey);
        return true;
    }

    @Transactional
    public void logBatch(Long userId, List<AuditLogRequest> logs) {
        logs.forEach(log -> logAudit(
                userId,
                log.getEntityType(),
                log.getEntityId(),
                log.getAction(),
                log.getOldValue(),
                log.getNewValue(),
                log.getDescription(),
                log.getIdempotencyKey()
        ));
    }

    public List<AuditLog> getAuditLogsForExport(Long userId,
                                                String entityType,
                                                Long entityId,
                                                String action,
                                                Instant startDate,
                                                Instant endDate) {
        return auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .filter(log -> userId == null || Objects.equals(log.getUserId(), userId))
                .filter(log -> entityType == null || entityType.isBlank() || entityType.equalsIgnoreCase(log.getEntityType()))
                .filter(log -> entityId == null || Objects.equals(log.getEntityId(), entityId))
                .filter(log -> action == null || action.isBlank() || action.equalsIgnoreCase(log.getAction()))
                .filter(log -> startDate == null || (log.getCreatedAt() != null && !log.getCreatedAt().isBefore(startDate)))
                .filter(log -> endDate == null || (log.getCreatedAt() != null && !log.getCreatedAt().isAfter(endDate)))
                .sorted(Comparator.comparing(AuditLog::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    public List<AuditLog> getAuditsByUserId(Long userId) {
        return auditLogRepository.findByUserId(userId);
    }

    public List<AuditLog> getAuditsByEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    public Page<AuditLog> getAuditsByDateRange(Instant startDate, Instant endDate, Pageable pageable) {
        return auditLogRepository.findByCreatedAtBetween(startDate, endDate, pageable);
    }

    public Page<AuditLog> getAuditsByUserAndDateRange(Long userId, Instant startDate, Instant endDate, Pageable pageable) {
        return auditLogRepository.findByUserIdAndCreatedAtBetween(userId, startDate, endDate, pageable);
    }

    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return null;
    }

    private String getUserAgent() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            return attributes.getRequest().getHeader("User-Agent");
        }
        return null;
    }
}

