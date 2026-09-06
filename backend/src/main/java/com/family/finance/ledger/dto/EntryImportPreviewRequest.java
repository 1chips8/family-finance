package com.family.finance.ledger.dto;

import jakarta.validation.constraints.NotBlank;

public record EntryImportPreviewRequest(@NotBlank(message = "CSV 内容不能为空") String csv) {}
