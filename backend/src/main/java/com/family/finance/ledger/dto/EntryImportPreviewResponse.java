package com.family.finance.ledger.dto;

import com.family.finance.ledger.domain.LedgerType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record EntryImportPreviewResponse(String checksum, int totalRows, int validRows, int errorRows,
                                         List<Row> rows) {
    public record Row(int rowNumber, LocalDate occurredOn, LedgerType type, Long categoryId,
                      String categoryName, Long memberId, String memberNo, BigDecimal amount,
                      String note, Map<String, String> errors) {
        public boolean valid() { return errors == null || errors.isEmpty(); }
    }
}
