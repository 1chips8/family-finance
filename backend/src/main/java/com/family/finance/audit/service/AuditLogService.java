package com.family.finance.audit.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.family.finance.audit.domain.AuditLog;
import com.family.finance.audit.dto.AuditLogResponse;
import com.family.finance.audit.mapper.AuditLogMapper;
import com.family.finance.auth.domain.AppUser;
import com.family.finance.auth.mapper.AppUserMapper;
import com.family.finance.common.security.CurrentUser;
import com.family.finance.common.security.CurrentUserService;
import com.family.finance.common.web.PageResponse;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AuditLogService {
    private final AuditLogMapper auditLogMapper;
    private final AppUserMapper userMapper;
    private final CurrentUserService currentUserService;

    public AuditLogService(AuditLogMapper auditLogMapper, AppUserMapper userMapper,
                           CurrentUserService currentUserService) {
        this.auditLogMapper = auditLogMapper;
        this.userMapper = userMapper;
        this.currentUserService = currentUserService;
    }

    public void record(CurrentUser actor, String action, String objectType, Long objectId, String summary) {
        if (actor == null || actor.householdId() == null) return;
        AuditLog log = new AuditLog();
        log.setHouseholdId(actor.householdId());
        log.setActorId(actor.id());
        log.setAction(action);
        log.setObjectType(objectType);
        log.setObjectId(objectId);
        log.setSummary(safeSummary(summary));
        auditLogMapper.insert(log);
    }

    public PageResponse<AuditLogResponse> list(long pageNumber, long pageSize) {
        CurrentUser parent = currentUserService.requireParent();
        long safePage = Math.max(pageNumber, 1);
        long safeSize = Math.min(Math.max(pageSize, 1), 100);
        Page<AuditLog> page = auditLogMapper.selectPage(new Page<>(safePage, safeSize),
                new QueryWrapper<AuditLog>().eq("household_id", parent.householdId())
                        .orderByDesc("created_at", "id"));
        Map<Long, AppUser> users = userMap(page.getRecords());
        return PageResponse.of(page.getRecords().stream()
                        .map(log -> AuditLogResponse.from(log,
                                users.get(log.getActorId()) == null ? "已停用成员" : users.get(log.getActorId()).getDisplayName()))
                        .toList(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    private Map<Long, AppUser> userMap(java.util.List<AuditLog> logs) {
        if (logs.isEmpty()) return Collections.emptyMap();
        return userMapper.selectBatchIds(logs.stream().map(AuditLog::getActorId).distinct().toList())
                .stream().collect(Collectors.toMap(AppUser::getId, Function.identity()));
    }

    private String safeSummary(String summary) {
        if (summary == null || summary.isBlank()) return "完成操作";
        String value = summary.strip();
        return value.length() <= 255 ? value : value.substring(0, 255);
    }
}
