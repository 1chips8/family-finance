package com.family.finance.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 4, max = 32, message = "用户名长度应为4-32位")
        @Pattern(regexp = "[A-Za-z0-9_]+", message = "用户名只能包含字母、数字和下划线")
        String username,
        @NotBlank(message = "姓名不能为空")
        @Size(max = 40, message = "姓名不能超过40个字符")
        String displayName,
        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 72, message = "密码长度应为8-72位")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码必须同时包含字母和数字")
        String password
) {}
