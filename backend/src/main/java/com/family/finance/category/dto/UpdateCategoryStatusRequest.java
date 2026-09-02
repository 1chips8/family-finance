package com.family.finance.category.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateCategoryStatusRequest(@NotNull(message = "状态不能为空") Boolean active) {}
