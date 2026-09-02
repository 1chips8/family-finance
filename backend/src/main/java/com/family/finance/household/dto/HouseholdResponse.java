package com.family.finance.household.dto;

import com.family.finance.auth.domain.Role;
import com.family.finance.common.security.CurrentUser;
import com.family.finance.household.domain.Household;

public record HouseholdResponse(
        Long id,
        String name,
        String inviteCode,
        Long currentMemberId,
        String currentMemberNo,
        Role currentRole
) {
    public static HouseholdResponse from(Household household, CurrentUser user) {
        return new HouseholdResponse(household.getId(), household.getName(),
                user.isParent() ? household.getInviteCode() : null, user.id(), user.memberNo(), user.role());
    }
}
