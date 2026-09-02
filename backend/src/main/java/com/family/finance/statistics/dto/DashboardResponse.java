package com.family.finance.statistics.dto;

import com.family.finance.ledger.dto.EntryResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
        LocalDate from,
        LocalDate to,
        Totals totals,
        List<TrendPoint> trend,
        List<CategoryBreakdown> composition,
        List<MemberComparison> members,
        List<EntryResponse> recentEntries
) {
    public record Totals(BigDecimal income, BigDecimal expense, BigDecimal balance) {}
    public record TrendPoint(String month, BigDecimal income, BigDecimal expense) {}
    public record CategoryBreakdown(Long categoryId, String categoryName, String type, BigDecimal amount) {}
    public record MemberComparison(Long memberId, String memberNo, String memberName,
                                   BigDecimal income, BigDecimal expense, BigDecimal balance) {}
}
