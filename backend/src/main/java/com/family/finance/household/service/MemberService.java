package com.family.finance.household.service;

import com.family.finance.auth.domain.AppUser;
import com.family.finance.auth.domain.Role;
import com.family.finance.auth.domain.UserStatus;
import com.family.finance.auth.mapper.AppUserMapper;
import com.family.finance.common.error.ApiException;
import com.family.finance.common.security.CurrentUser;
import com.family.finance.common.security.CurrentUserService;
import com.family.finance.household.dto.MemberResponse;
import com.family.finance.household.dto.UpdateMemberRequest;
import com.family.finance.household.dto.UpdateMemberStatusRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MemberService {
    private final AppUserMapper userMapper;
    private final CurrentUserService currentUserService;

    public MemberService(AppUserMapper userMapper, CurrentUserService currentUserService) {
        this.userMapper = userMapper;
        this.currentUserService = currentUserService;
    }

    public List<MemberResponse> list() {
        CurrentUser current = currentUserService.requireHouseholdUser();
        return userMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AppUser>()
                        .eq("household_id", current.householdId()).orderByAsc("member_no"))
                .stream().map(MemberResponse::from).toList();
    }

    @Transactional
    public MemberResponse update(Long id, UpdateMemberRequest request) {
        CurrentUser operator = currentUserService.requireParent();
        AppUser target = targetInHousehold(id, operator.householdId());
        Role oldRole = target.getRole();
        if (oldRole == Role.PARENT && request.role() != Role.PARENT && target.getStatus() == UserStatus.ACTIVE
                && userMapper.countActiveParents(operator.householdId()) <= 1) {
            throw new ApiException(HttpStatus.CONFLICT, "LAST_PARENT_PROTECTED", "至少需要保留一名活跃家长");
        }
        target.setMemberNo(request.memberNo().trim().toUpperCase());
        target.setDisplayName(request.displayName().trim());
        target.setRole(request.role());
        userMapper.updateById(target);
        return MemberResponse.from(target);
    }

    @Transactional
    public MemberResponse updateStatus(Long id, UpdateMemberStatusRequest request) {
        CurrentUser operator = currentUserService.requireParent();
        AppUser target = targetInHousehold(id, operator.householdId());
        if (!request.active() && target.getRole() == Role.PARENT && target.getStatus() == UserStatus.ACTIVE
                && userMapper.countActiveParents(operator.householdId()) <= 1) {
            throw new ApiException(HttpStatus.CONFLICT, "LAST_PARENT_PROTECTED", "至少需要保留一名活跃家长");
        }
        target.setStatus(request.active() ? UserStatus.ACTIVE : UserStatus.INACTIVE);
        userMapper.updateById(target);
        return MemberResponse.from(target);
    }

    private AppUser targetInHousehold(Long id, Long householdId) {
        AppUser target = userMapper.selectById(id);
        if (target == null || !householdId.equals(target.getHouseholdId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "成员不存在");
        }
        return target;
    }
}
