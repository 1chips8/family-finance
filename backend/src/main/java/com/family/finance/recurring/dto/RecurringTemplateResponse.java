package com.family.finance.recurring.dto;

import com.family.finance.ledger.domain.LedgerType;
import com.family.finance.recurring.domain.RecurringStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RecurringTemplateResponse(
        Long id,
        Long memberId,
        String memberName,
        String memberNo,
        LedgerType type,
        Long categoryId,
        String categoryName,
        BigDecimal amount,
        Integer dayOfMonth,
        String note,
        RecurringStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    @JsonProperty("active")
    public boolean active() { return status == RecurringStatus.ACTIVE; }
}
