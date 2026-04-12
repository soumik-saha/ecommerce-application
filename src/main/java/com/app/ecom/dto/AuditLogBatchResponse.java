package com.app.ecom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogBatchResponse {
    private int totalProcessed;
    private int successCount;
    private int duplicateCount;
    private int failureCount;
    private List<String> failedRecords;
}

