package com.family.finance.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePasswordRequest(
        @NotBlank(message = "旧密码不能为空") String currentPassword,
        @NotBlank(message = "新密码不能为空") @Size(min = 8, max = 72, message = "密码长度应为8-72位")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码必须同时包含字母和数字") String newPassword
) {}
