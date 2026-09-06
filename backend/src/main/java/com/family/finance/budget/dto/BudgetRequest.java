package com.family.finance.budget.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BudgetRequest(
        String month,
        @NotNull(message = "预算金额不能为空")
        @DecimalMin(value = "0.01", message = "预算金额必须大于0")
        @Digits(integer = 10, fraction = 2, message = "预算金额最多10位整数和2位小数")
        BigDecimal amount
) {
    /** Allows service-level callers to construct a request without a transport month. */
    public BudgetRequest(BigDecimal amount) { this(null, amount); }
}
