package com.family.finance.ledger.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("ledger_entry")
public class LedgerEntry {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long householdId;
    private Long memberId;
    private Long categoryId;
    private LedgerType type;
    private BigDecimal amount;
    private LocalDate occurredOn;
    private String note;
    @TableLogic(value = "0", delval = "1")
    private Boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
