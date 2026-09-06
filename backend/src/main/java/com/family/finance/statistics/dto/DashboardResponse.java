package com.family.finance.statistics.dto;

import com.family.finance.ledger.dto.EntryResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.family.finance.budget.dto.BudgetOverviewResponse;

public record DashboardResponse(
        LocalDate from,
        LocalDate to,
        Totals totals,
        List<TrendPoint> trend,
        List<CategoryBreakdown> composition,
        List<MemberComparison> members,
        List<EntryResponse> recentEntries,
        Comparison comparison,
        BigDecimal savingsRate,
        BudgetOverviewResponse budget,
        List<Anomaly> anomalies
) {
    /** Backward-compatible constructor for callers that only need the original dashboard. */
    public DashboardResponse(LocalDate from, LocalDate to, Totals totals, List<TrendPoint> trend,
                             List<CategoryBreakdown> composition, List<MemberComparison> members,
                             List<EntryResponse> recentEntries) {
        this(from, to, totals, trend, composition, members, recentEntries, null, null, null, List.of());
    }

    public record Totals(BigDecimal income, BigDecimal expense, BigDecimal balance) {}
    public record TrendPoint(String month, BigDecimal income, BigDecimal expense) {}
    public record CategoryBreakdown(Long categoryId, String categoryName, String type, BigDecimal amount) {}
    public record MemberComparison(Long memberId, String memberNo, String memberName,
                                   BigDecimal income, BigDecimal expense, BigDecimal balance) {}

    /** Previous equal-length period and absolute changes for income, expense and balance. */
    public record Comparison(BigDecimal income, BigDecimal expense, BigDecimal balance,
                             Totals previous, BigDecimal incomeChangeRate,
                             BigDecimal expenseChangeRate, BigDecimal balanceChangeRate) {
        public Comparison(BigDecimal income, BigDecimal expense, BigDecimal balance) {
            this(income, expense, balance, null, null, null, null);
        }
        public BigDecimal incomeRate() { return incomeChangeRate; }
        public BigDecimal expenseRate() { return expenseChangeRate; }
        public BigDecimal balanceRate() { return balanceChangeRate; }
    }

    public record Anomaly(Long categoryId, String categoryName, BigDecimal currentAmount,
                          BigDecimal baselineAmount, BigDecimal ratio) {
        public BigDecimal averageAmount() { return baselineAmount; }
        public BigDecimal growthRate() { return ratio; }
        public String message() {
            return categoryName + "支出较近三个月月均增长超过50%";
        }
    }
}
