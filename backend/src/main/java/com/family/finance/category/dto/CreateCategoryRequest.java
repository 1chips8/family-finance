package com.family.finance.category.dto;

import com.family.finance.category.domain.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotNull(message = "分类类型不能为空") CategoryType type,
        @NotBlank(message = "分类名称不能为空") @Size(max = 30, message = "分类名称不能超过30个字符") String name
) {}
