package com.family.finance.budget.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Budget configuration together with the live ledger usage for a month.
 * All monetary values use two decimal places; usage rates are ratios (1.0 = 100%).
 */
public record BudgetOverviewResponse(
        YearMonth month,
        BudgetItem total,
        List<BudgetItem> categories
) {
    public record BudgetItem(
            Long id,
            Long categoryId,
            String categoryName,
            BigDecimal budget,
            BigDecimal spent,
            BigDecimal remaining,
            BigDecimal usageRate,
            BudgetStatus status
    ) {}

    /** Alias retained for callers that use the more specific category terminology. */
    public record CategoryBudget(
            Long categoryId, String categoryName, BigDecimal budget, BigDecimal spent,
            BigDecimal remaining, BigDecimal usageRate, BudgetStatus status
    ) {
        public BudgetItem asItem() {
            return new BudgetItem(null, categoryId, categoryName, budget, spent, remaining, usageRate, status);
        }
    }

    public enum BudgetStatus { NORMAL, WARNING, OVER }

    /** Convenience aliases for callers that describe the total as a summary. */
    public BigDecimal budgetAmount() { return total == null ? BigDecimal.ZERO : total.budget(); }
    public BigDecimal spentAmount() { return total == null ? BigDecimal.ZERO : total.spent(); }
    public BigDecimal remainingAmount() { return total == null ? BigDecimal.ZERO : total.remaining(); }
    public BigDecimal usageRate() { return total == null ? null : total.usageRate(); }
    public BudgetStatus status() { return total == null ? BudgetStatus.NORMAL : total.status(); }
    public List<BudgetItem> categoryBudgets() { return categories; }
}
