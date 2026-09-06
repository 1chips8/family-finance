package com.family.finance.recurring.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateRecurringStatusRequest(@NotNull(message = "状态不能为空") Boolean active) {}
