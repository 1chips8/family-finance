package com.family.finance.household.dto;

import com.family.finance.auth.domain.Role;
import com.family.finance.auth.domain.UserStatus;
import com.family.finance.auth.domain.AppUser;

import java.time.LocalDateTime;

public record MemberResponse(Long id, String memberNo, String displayName, Role role,
                             UserStatus status, LocalDateTime joinedAt) {
    public static MemberResponse from(AppUser user) {
        return new MemberResponse(user.getId(), user.getMemberNo(), user.getDisplayName(), user.getRole(),
                user.getStatus(), user.getCreatedAt());
    }
}
