package com.family.finance.budget.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** A household budget for one natural month and, optionally, one expense category. */
@Data
@TableName("monthly_budget")
public class MonthlyBudget {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long householdId;
    private LocalDate budgetMonth;
    private Long categoryId;
    private BigDecimal amount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
