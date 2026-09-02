package com.family.finance.category.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("finance_category")
public class FinanceCategory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long householdId;
    private CategoryScope scope;
    private CategoryType type;
    private String name;
    private CategoryStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
