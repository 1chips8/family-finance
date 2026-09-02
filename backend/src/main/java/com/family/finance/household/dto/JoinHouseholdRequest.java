package com.family.finance.household.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record JoinHouseholdRequest(
        @NotBlank(message = "邀请码不能为空")
        @Pattern(regexp = "[A-Za-z0-9]{8}", message = "邀请码应为8位字母或数字")
        String inviteCode
) {}
