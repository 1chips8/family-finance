package com.family.finance.audit.dto;

import com.family.finance.audit.domain.AuditLog;

import java.time.LocalDateTime;

public record AuditLogResponse(Long id, Long actorId, String actorName, String action,
                               String objectType, Long objectId, String summary, LocalDateTime createdAt) {
    public static AuditLogResponse from(AuditLog log, String actorName) {
        return new AuditLogResponse(log.getId(), log.getActorId(), actorName, log.getAction(),
                log.getObjectType(), log.getObjectId(), log.getSummary(), log.getCreatedAt());
    }
}
