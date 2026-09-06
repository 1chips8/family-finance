package com.family.finance.audit.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long householdId;
    private Long actorId;
    private String action;
    private String objectType;
    private Long objectId;
    private String summary;
    private LocalDateTime createdAt;
}
