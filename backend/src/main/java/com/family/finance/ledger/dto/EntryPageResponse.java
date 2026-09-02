package com.family.finance.ledger.dto;

import com.family.finance.common.web.PageResponse;

import java.util.List;

public record EntryPageResponse(List<EntryResponse> items, long page, long pageSize, long total, long totalPages) {
    public static EntryPageResponse from(PageResponse<EntryResponse> page) {
        return new EntryPageResponse(page.items(), page.page(), page.pageSize(), page.total(), page.totalPages());
    }
}
