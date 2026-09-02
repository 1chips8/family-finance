package com.family.finance.ledger.dto;

import com.family.finance.ledger.domain.LedgerEntry;
import com.family.finance.ledger.domain.LedgerType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EntryResponse(Long id, Long memberId, String memberName, String memberNo, LedgerType type,
                            Long categoryId, String categoryName, BigDecimal amount, LocalDate occurredOn,
                            String note, LocalDateTime createdAt) {
    public static EntryResponse from(LedgerEntry entry, String memberName, String memberNo, String categoryName) {
        return new EntryResponse(entry.getId(), entry.getMemberId(), memberName, memberNo, entry.getType(),
                entry.getCategoryId(), categoryName, entry.getAmount(), entry.getOccurredOn(), entry.getNote(), entry.getCreatedAt());
    }
}
