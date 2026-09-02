package com.family.finance.household.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateMemberStatusRequest(@NotNull(message = "状态不能为空") Boolean active) {}
