package com.family.finance.household;

import com.family.finance.auth.domain.AppUser;
import com.family.finance.auth.domain.Role;
import com.family.finance.auth.domain.UserStatus;
import com.family.finance.auth.mapper.AppUserMapper;
import com.family.finance.audit.service.AuditLogService;
import com.family.finance.common.error.ApiException;
import com.family.finance.common.security.CurrentUser;
import com.family.finance.common.security.CurrentUserService;
import com.family.finance.household.dto.UpdateMemberRequest;
import com.family.finance.household.mapper.HouseholdMapper;
import com.family.finance.household.service.MemberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {
    @Mock AppUserMapper userMapper;
    @Mock HouseholdMapper householdMapper;
    @Mock CurrentUserService currentUserService;
    @Mock AuditLogService auditLogService;
    @InjectMocks MemberService memberService;

    @Test
    void rejectsDemotingTheLastActiveParent() {
        CurrentUser parent = new CurrentUser(1L, "parent", "家长", 9L, "M001", Role.PARENT, UserStatus.ACTIVE);
        AppUser target = new AppUser(); target.setId(1L); target.setHouseholdId(9L); target.setRole(Role.PARENT); target.setStatus(UserStatus.ACTIVE);
        when(currentUserService.requireParent()).thenReturn(parent);
        when(userMapper.selectById(1L)).thenReturn(target);
        when(userMapper.countActiveParents(9L)).thenReturn(1L);

        assertThatThrownBy(() -> memberService.update(1L, new UpdateMemberRequest("M001", "家长", Role.MEMBER)))
                .isInstanceOf(ApiException.class)
                .hasMessage("至少需要保留一名活跃家长");
        verify(userMapper, never()).updateById(any(AppUser.class));
        verify(householdMapper).selectForUpdateById(9L);
    }
}
