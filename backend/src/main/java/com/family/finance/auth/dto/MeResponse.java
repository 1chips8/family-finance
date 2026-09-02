package com.family.finance.auth.dto;

import com.family.finance.auth.domain.Role;
import com.family.finance.auth.domain.UserStatus;
import com.family.finance.common.security.CurrentUser;

public record MeResponse(
        Long id,
        String username,
        String displayName,
        String memberNo,
        Role role,
        UserStatus status,
        boolean hasHousehold,
        HouseholdSummary household
) {
    public static MeResponse from(CurrentUser user, HouseholdSummary household) {
        return new MeResponse(user.id(), user.username(), user.displayName(), user.memberNo(), user.role(),
                user.status(), user.hasHousehold(), household);
    }

    public record HouseholdSummary(Long id, String name) {}
}
