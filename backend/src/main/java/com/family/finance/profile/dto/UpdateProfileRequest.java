package com.family.finance.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "姓名不能为空") @Size(max = 40, message = "姓名不能超过40个字符") String displayName
) {}
