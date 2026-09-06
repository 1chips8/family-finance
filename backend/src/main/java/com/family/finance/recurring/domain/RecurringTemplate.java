package com.family.finance.recurring.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.family.finance.ledger.domain.LedgerType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("recurring_template")
public class RecurringTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long householdId;
    private Long memberId;
    private Long categoryId;
    private LedgerType type;
    private BigDecimal amount;
    private Integer dayOfMonth;
    private String note;
    private RecurringStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
