package com.family.finance.ledger.dto;

import com.family.finance.ledger.domain.LedgerType;

import java.time.LocalDate;

public record EntryQuery(LocalDate from, LocalDate to, LedgerType type, Long categoryId, Long memberId,
                         long page, long pageSize) {
    public EntryQuery normalized() {
        return new EntryQuery(from, to, type, categoryId, memberId,
                Math.max(page, 1), Math.min(Math.max(pageSize, 1), 100));
    }
}
