package com.family.finance.recurring.dto;

import com.family.finance.ledger.domain.LedgerType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecurringTemplateRequest(
        Long memberId,
        @NotNull(message = "收支类型不能为空") LedgerType type,
        @NotNull(message = "分类不能为空") Long categoryId,
        @NotNull(message = "金额不能为空")
        @DecimalMin(value = "0.01", message = "金额必须大于0")
        @Digits(integer = 10, fraction = 2, message = "金额最多10位整数和2位小数") BigDecimal amount,
        @NotNull(message = "发生日不能为空")
        @Min(value = 1, message = "发生日必须在1到31之间")
        @Max(value = 31, message = "发生日必须在1到31之间") Integer dayOfMonth,
        @Size(max = 255, message = "备注不能超过255个字符") String note
) {}
