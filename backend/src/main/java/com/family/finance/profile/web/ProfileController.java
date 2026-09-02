package com.family.finance.profile.web;

import com.family.finance.auth.dto.MeResponse;
import com.family.finance.auth.mapper.AppUserMapper;
import com.family.finance.common.security.CurrentUser;
import com.family.finance.common.web.ApiResponse;
import com.family.finance.profile.dto.UpdatePasswordRequest;
import com.family.finance.profile.dto.UpdateProfileRequest;
import com.family.finance.profile.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService profileService;
    private final com.family.finance.auth.service.AuthService authService;

    public ProfileController(ProfileService profileService, com.family.finance.auth.service.AuthService authService) {
        this.profileService = profileService;
        this.authService = authService;
    }

    @PatchMapping
    public ApiResponse<MeResponse> update(@Valid @RequestBody UpdateProfileRequest request) {
        profileService.update(request);
        return ApiResponse.of(authService.me());
    }

    @PutMapping("/password")
    public ApiResponse<Map<String, String>> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        profileService.updatePassword(request);
        return ApiResponse.of(Map.of("message", "密码修改成功，请重新登录"));
    }
}
