package com.family.finance.household.service;

import com.family.finance.auth.domain.AppUser;
import com.family.finance.auth.domain.Role;
import com.family.finance.auth.mapper.AppUserMapper;
import com.family.finance.common.error.ApiException;
import com.family.finance.common.security.CurrentUser;
import com.family.finance.common.security.CurrentUserService;
import com.family.finance.household.domain.Household;
import com.family.finance.household.dto.CreateHouseholdRequest;
import com.family.finance.household.dto.HouseholdResponse;
import com.family.finance.household.dto.JoinHouseholdRequest;
import com.family.finance.household.mapper.HouseholdMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
public class HouseholdService {
    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private final SecureRandom secureRandom = new SecureRandom();
    private final HouseholdMapper householdMapper;
    private final AppUserMapper userMapper;
    private final CurrentUserService currentUserService;

    public HouseholdService(HouseholdMapper householdMapper, AppUserMapper userMapper,
                            CurrentUserService currentUserService) {
        this.householdMapper = householdMapper;
        this.userMapper = userMapper;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public HouseholdResponse create(CreateHouseholdRequest request) {
        CurrentUser current = currentUserService.requireUser();
        if (current.hasHousehold()) throw new ApiException(HttpStatus.CONFLICT, "ALREADY_IN_HOUSEHOLD", "你已经属于一个家庭");
        Household household = new Household();
        household.setName(request.name().trim());
        household.setInviteCode(uniqueInviteCode());
        household.setCreatedBy(current.id());
        householdMapper.insert(household);

        AppUser user = new AppUser();
        user.setId(current.id());
        user.setHouseholdId(household.getId());
        user.setMemberNo("M001");
        user.setRole(Role.PARENT);
        userMapper.updateById(user);
        return HouseholdResponse.from(household, new CurrentUser(current.id(), current.username(), current.displayName(),
                household.getId(), "M001", Role.PARENT, current.status()));
    }

    @Transactional
    public HouseholdResponse join(JoinHouseholdRequest request) {
        CurrentUser current = currentUserService.requireUser();
        if (current.hasHousehold()) throw new ApiException(HttpStatus.CONFLICT, "ALREADY_IN_HOUSEHOLD", "你已经属于一个家庭");
        String code = request.inviteCode().trim().toUpperCase();
        Household found = householdMapper.selectByInviteCode(code);
        if (found == null) throw new ApiException(HttpStatus.NOT_FOUND, "INVITE_CODE_INVALID", "邀请码无效或已失效");
        Household household = householdMapper.selectForUpdateById(found.getId());
        if (household == null || !code.equals(household.getInviteCode())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "INVITE_CODE_INVALID", "邀请码无效或已失效");
        }
        int sequence = userMapper.maxMemberSequenceForUpdate(household.getId()) + 1;
        String memberNo = "M" + String.format("%03d", sequence);
        AppUser user = new AppUser();
        user.setId(current.id());
        user.setHouseholdId(household.getId());
        user.setMemberNo(memberNo);
        user.setRole(Role.MEMBER);
        userMapper.updateById(user);
        return HouseholdResponse.from(household, new CurrentUser(current.id(), current.username(), current.displayName(),
                household.getId(), memberNo, Role.MEMBER, current.status()));
    }

    public HouseholdResponse current() {
        CurrentUser user = currentUserService.requireHouseholdUser();
        Household household = householdMapper.selectById(user.householdId());
        if (household == null) throw new ApiException(HttpStatus.NOT_FOUND, "HOUSEHOLD_NOT_FOUND", "家庭不存在");
        return HouseholdResponse.from(household, user);
    }

    @Transactional
    public HouseholdResponse rotateInviteCode() {
        CurrentUser user = currentUserService.requireParent();
        Household household = householdMapper.selectForUpdateById(user.householdId());
        household.setInviteCode(uniqueInviteCode());
        householdMapper.updateById(household);
        return HouseholdResponse.from(household, user);
    }

    private String uniqueInviteCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder code = new StringBuilder(8);
            for (int i = 0; i < 8; i++) code.append(CODE_CHARS[secureRandom.nextInt(CODE_CHARS.length)]);
            if (householdMapper.selectByInviteCode(code.toString()) == null) return code.toString();
        }
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INVITE_CODE_GENERATION_FAILED", "邀请码生成失败，请重试");
    }
}
