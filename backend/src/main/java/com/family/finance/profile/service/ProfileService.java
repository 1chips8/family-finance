package com.family.finance.profile.service;

import com.family.finance.auth.domain.AppUser;
import com.family.finance.auth.mapper.AppUserMapper;
import com.family.finance.common.error.ApiException;
import com.family.finance.common.security.CurrentUser;
import com.family.finance.common.security.CurrentUserService;
import com.family.finance.profile.dto.UpdatePasswordRequest;
import com.family.finance.profile.dto.UpdateProfileRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
    private final AppUserMapper userMapper;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(AppUserMapper userMapper, CurrentUserService currentUserService, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CurrentUser update(UpdateProfileRequest request) {
        CurrentUser current = currentUserService.requireUser();
        AppUser user = userMapper.selectById(current.id());
        user.setDisplayName(request.displayName().trim());
        userMapper.updateById(user);
        return new CurrentUser(user.getId(), user.getUsername(), user.getDisplayName(), user.getHouseholdId(),
                user.getMemberNo(), user.getRole(), user.getStatus());
    }

    @Transactional
    public void updatePassword(UpdatePasswordRequest request) {
        CurrentUser current = currentUserService.requireUser();
        AppUser user = userMapper.selectById(current.id());
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CURRENT_PASSWORD_INVALID", "旧密码不正确");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(user);
    }
}
