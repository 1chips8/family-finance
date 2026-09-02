package com.family.finance.ledger.dto;

import com.family.finance.ledger.domain.LedgerType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateEntryRequest(
        Long memberId,
        @NotNull(message = "收支类型不能为空") LedgerType type,
        @NotNull(message = "分类不能为空") Long categoryId,
        @NotNull(message = "金额不能为空") @DecimalMin(value = "0.01", message = "金额必须大于0")
        @Digits(integer = 10, fraction = 2, message = "金额最多10位整数和2位小数") BigDecimal amount,
        @NotNull(message = "发生日期不能为空") @PastOrPresent(message = "发生日期不能晚于今天") LocalDate occurredOn,
        @Size(max = 255, message = "备注不能超过255个字符") String note
) {}
