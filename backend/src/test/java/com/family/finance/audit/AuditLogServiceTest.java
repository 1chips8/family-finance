package com.family.finance.audit;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.family.finance.audit.domain.AuditLog;
import com.family.finance.audit.mapper.AuditLogMapper;
import com.family.finance.audit.service.AuditLogService;
import com.family.finance.auth.domain.AppUser;
import com.family.finance.auth.domain.Role;
import com.family.finance.auth.domain.UserStatus;
import com.family.finance.auth.mapper.AppUserMapper;
import com.family.finance.common.security.CurrentUser;
import com.family.finance.common.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {
    @Mock AuditLogMapper auditLogMapper;
    @Mock AppUserMapper userMapper;
    @Mock CurrentUserService currentUserService;

    @Test
    void recordsOnlyServerAuthoredHouseholdSummary() {
        AuditLogService service = new AuditLogService(auditLogMapper, userMapper, currentUserService);
        CurrentUser actor = new CurrentUser(2L, "member", "成员", 9L, "M002", Role.MEMBER, UserStatus.ACTIVE);

        service.record(actor, "ENTRY_CREATE", "ENTRY", 7L, "新增支出流水 ¥88.88");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getHouseholdId()).isEqualTo(9L);
        assertThat(captor.getValue().getActorId()).isEqualTo(2L);
        assertThat(captor.getValue().getSummary()).isEqualTo("新增支出流水 ¥88.88");
    }

    @SuppressWarnings("unchecked")
    @Test
    void listsOnlyTheCurrentParentsHousehold() {
        AuditLogService service = new AuditLogService(auditLogMapper, userMapper, currentUserService);
        CurrentUser parent = new CurrentUser(1L, "parent", "家长", 9L, "M001", Role.PARENT, UserStatus.ACTIVE);
        when(currentUserService.requireParent()).thenReturn(parent);
        AuditLog log = new AuditLog();
        log.setId(4L); log.setHouseholdId(9L); log.setActorId(1L); log.setAction("BUDGET_UPDATE");
        log.setObjectType("BUDGET"); log.setSummary("更新九月总预算"); log.setCreatedAt(LocalDateTime.now());
        when(auditLogMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(invocation -> {
            Page<AuditLog> page = invocation.getArgument(0);
            page.setRecords(List.of(log)); page.setTotal(1); return page;
        });
        AppUser actor = new AppUser(); actor.setId(1L); actor.setDisplayName("家长");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(actor));

        var result = service.list(1, 20);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items().get(0).actorName()).isEqualTo("家长");
        verify(currentUserService).requireParent();
    }
}
