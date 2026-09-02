package com.family.finance.household.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateHouseholdRequest(
        @NotBlank(message = "家庭名称不能为空")
        @Size(max = 80, message = "家庭名称不能超过80个字符")
        String name
) {}
