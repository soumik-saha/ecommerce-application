package com.app.ecom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogRequest {
    @JsonProperty("entityType")
    @NotBlank(message = "entityType is required")
    private String entityType;

    @JsonProperty("entityId")
    @NotNull(message = "entityId is required")
    private Long entityId;

    @JsonProperty("action")
    @NotBlank(message = "action is required")
    private String action;

    @JsonProperty("description")
    private String description;

    @JsonProperty("oldValue")
    private String oldValue;

    @JsonProperty("newValue")
    private String newValue;

    @JsonProperty("idempotencyKey")
    private String idempotencyKey;
}
