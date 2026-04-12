package com.app.ecom.dto;

import com.app.ecom.model.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDTO {
	private Long id;
	private Long userId;
	private String entityType;
	private Long entityId;
	private String action;
	private String oldValue;
	private String newValue;
	private String description;
	private String ipAddress;
	private String userAgent;
	private String idempotencyKey;
	private Instant createdAt;

	public static AuditLogDTO from(AuditLog auditLog) {
		return AuditLogDTO.builder()
				.id(auditLog.getId())
				.userId(auditLog.getUserId())
				.entityType(auditLog.getEntityType())
				.entityId(auditLog.getEntityId())
				.action(auditLog.getAction())
				.oldValue(auditLog.getOldValue())
				.newValue(auditLog.getNewValue())
				.description(auditLog.getDescription())
				.ipAddress(auditLog.getIpAddress())
				.userAgent(auditLog.getUserAgent())
				.idempotencyKey(auditLog.getIdempotencyKey())
				.createdAt(auditLog.getCreatedAt())
				.build();
	}

	public static String csvHeader() {
		return String.join(",",
				"id",
				"userId",
				"entityType",
				"entityId",
				"action",
				"oldValue",
				"newValue",
				"description",
				"ipAddress",
				"userAgent",
				"idempotencyKey",
				"createdAt");
	}

	public String toCsvRow() {
		return String.join(",",
				csvValue(id),
				csvValue(userId),
				csvValue(entityType),
				csvValue(entityId),
				csvValue(action),
				csvValue(oldValue),
				csvValue(newValue),
				csvValue(description),
				csvValue(ipAddress),
				csvValue(userAgent),
				csvValue(idempotencyKey),
				csvValue(createdAt));
	}

	private static String csvValue(Object value) {
		if (value == null) {
			return "";
		}

		String text = value instanceof Instant instant ? instant.toString() : String.valueOf(value);
		if (text.contains("\"") || text.contains(",") || text.contains("\n") || text.contains("\r")) {
			return '"' + text.replace("\"", "\"\"") + '"';
		}
		return text;
	}
}
