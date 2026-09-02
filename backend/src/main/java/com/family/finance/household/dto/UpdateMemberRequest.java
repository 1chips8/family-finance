package com.family.finance.household.dto;

import com.family.finance.auth.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMemberRequest(
        @NotBlank(message = "成员编号不能为空")
        @Pattern(regexp = "M\\d{3,}", message = "成员编号格式应为M加至少三位数字")
        String memberNo,
        @NotBlank(message = "姓名不能为空")
        @Size(max = 40, message = "姓名不能超过40个字符")
        String displayName,
        @NotNull(message = "角色不能为空") Role role
) {}
