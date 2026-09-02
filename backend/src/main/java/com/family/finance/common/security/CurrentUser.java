package com.family.finance.common.security;

import com.family.finance.auth.domain.Role;
import com.family.finance.auth.domain.UserStatus;

public record CurrentUser(
        Long id,
        String username,
        String displayName,
        Long householdId,
        String memberNo,
        Role role,
        UserStatus status
) {
    public boolean hasHousehold() { return householdId != null && role != null; }
    public boolean isParent() { return role == Role.PARENT; }
}
