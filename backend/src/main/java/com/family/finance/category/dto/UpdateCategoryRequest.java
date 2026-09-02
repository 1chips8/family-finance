package com.family.finance.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @NotBlank(message = "分类名称不能为空") @Size(max = 30, message = "分类名称不能超过30个字符") String name
) {}
