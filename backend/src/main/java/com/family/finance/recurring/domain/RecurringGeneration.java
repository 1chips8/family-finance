package com.family.finance.recurring.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("recurring_generation")
public class RecurringGeneration {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recurringTemplateId;
    private LocalDate generatedMonth;
    private Long ledgerEntryId;
    private LocalDateTime createdAt;
}
