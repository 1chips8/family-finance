package com.family.finance.ledger.dto;

import jakarta.validation.constraints.NotBlank;

public record EntryImportRequest(
        @NotBlank(message = "CSV 内容不能为空") String csv,
        @NotBlank(message = "预览校验码不能为空") String checksum
) {}
