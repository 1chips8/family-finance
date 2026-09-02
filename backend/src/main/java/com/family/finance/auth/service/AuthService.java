package com.family.finance.auth.service;

import com.family.finance.auth.domain.AppUser;
import com.family.finance.auth.dto.MeResponse;
import com.family.finance.auth.dto.RegisterRequest;
import com.family.finance.auth.mapper.AppUserMapper;
import com.family.finance.common.error.ApiException;
import com.family.finance.common.security.CurrentUser;
import com.family.finance.common.security.CurrentUserService;
import com.family.finance.household.domain.Household;
import com.family.finance.household.mapper.HouseholdMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AppUserMapper userMapper;
    private final HouseholdMapper householdMapper;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AppUserMapper userMapper, HouseholdMapper householdMapper,
                       CurrentUserService currentUserService, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.householdMapper = householdMapper;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegisterRequest request) {
        String username = request.username().trim();
        if (userMapper.selectByUsername(username) != null) {
            throw new ApiException(HttpStatus.CONFLICT, "USERNAME_TAKEN", "用户名已存在");
        }
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setDisplayName(request.displayName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(com.family.finance.auth.domain.UserStatus.ACTIVE);
        userMapper.insert(user);
    }

    public MeResponse me() {
        CurrentUser user = currentUserService.requireUser();
        return MeResponse.from(user, householdSummary(user.householdId()));
    }

    private MeResponse.HouseholdSummary householdSummary(Long householdId) {
        if (householdId == null) return null;
        Household household = householdMapper.selectById(householdId);
        return household == null ? null : new MeResponse.HouseholdSummary(household.getId(), household.getName());
    }
}
