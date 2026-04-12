package com.app.ecom.controller;

import com.app.ecom.dto.*;
import com.app.ecom.model.AuditLog;
import com.app.ecom.security.AppUserDetails;
import com.app.ecom.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Slf4j
public class AuditLogController {
    private final AuditLogService auditLogService;

    @PostMapping("/batch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuditLogBatchResponse> batchLogAudits(
            @Valid @RequestBody AuditLogBatchRequest request,
            @AuthenticationPrincipal AppUserDetails currentUser,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        try {
            Long userId = currentUser != null ? currentUser.getId() : null;

            List<AuditLogRequest> logs = request.getLogs();
            int totalProcessed = 0;
            int successCount = 0;
            int duplicateCount = 0;
            int failureCount = 0;
            List<String> failedRecords = new ArrayList<>();

            if (logs != null) {
                totalProcessed = logs.size();
                for (int i = 0; i < logs.size(); i++) {
                    try {
                        AuditLogRequest auditLogRequest = logs.get(i);
                        String effectiveIdempotencyKey = auditLogRequest.getIdempotencyKey();
                        if (effectiveIdempotencyKey == null || effectiveIdempotencyKey.isBlank()) {
                            effectiveIdempotencyKey = idempotencyKey;
                        }

                        boolean created = auditLogService.logAuditIfAbsent(
                                userId,
                                auditLogRequest.getEntityType(),
                                auditLogRequest.getEntityId(),
                                auditLogRequest.getAction(),
                                auditLogRequest.getOldValue(),
                                auditLogRequest.getNewValue(),
                                auditLogRequest.getDescription(),
                                effectiveIdempotencyKey
                        );
                        if (created) {
                            successCount++;
                        } else {
                            duplicateCount++;
                        }
                    } catch (Exception e) {
                        failureCount++;
                        failedRecords.add("Record " + (i + 1) + ": " + e.getMessage());
                        log.error("Error processing audit log at index {}: {}", i, e.getMessage());
                    }
                }
            }

            AuditLogBatchResponse response = AuditLogBatchResponse.builder()
                    .totalProcessed(totalProcessed)
                    .successCount(successCount)
                    .duplicateCount(duplicateCount)
                    .failureCount(failureCount)
                    .failedRecords(failedRecords)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing batch audit logs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuditLogBatchResponse.builder()
                            .totalProcessed(0)
                            .successCount(0)
                            .failureCount(0)
                            .failedRecords(List.of(e.getMessage()))
                            .build());
        }
    }

    @GetMapping(value = "/download", produces = "text/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> downloadAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Instant parsedStartDate = parseInstant(startDate, "startDate");
        Instant parsedEndDate = parseInstant(endDate, "endDate");

        List<AuditLog> audits = auditLogService.getAuditLogsForExport(
                userId,
                entityType,
                entityId,
                action,
                parsedStartDate,
                parsedEndDate);

        StringBuilder csv = new StringBuilder();
        csv.append(AuditLogDTO.csvHeader()).append('\n');
        for (AuditLog auditLog : audits) {
            csv.append(AuditLogDTO.from(auditLog).toCsvRow()).append('\n');
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit-logs.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AuditLog>> getAuditsByUser(@PathVariable Long userId) {
        List<AuditLog> audits = auditLogService.getAuditsByUserId(userId);
        return ResponseEntity.ok(audits);
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AuditLog>> getAuditsByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        List<AuditLog> audits = auditLogService.getAuditsByEntity(entityType, entityId);
        return ResponseEntity.ok(audits);
    }

    @GetMapping("/range")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLog>> getAuditsByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            Pageable pageable) {
        try {
            Instant start = Instant.parse(startDate);
            Instant end = Instant.parse(endDate);
            Page<AuditLog> audits = auditLogService.getAuditsByDateRange(start, end, pageable);
            return ResponseEntity.ok(audits);
        } catch (DateTimeParseException e) {
            log.error("Invalid date format: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> getAuditLogs() {
        return ResponseEntity.ok("{\"message\": \"Audit logs endpoint is working\"}");
    }

    private Instant parseInstant(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(fieldName + " must be an ISO-8601 instant, e.g. 2026-04-12T10:15:30Z");
        }
    }
}


