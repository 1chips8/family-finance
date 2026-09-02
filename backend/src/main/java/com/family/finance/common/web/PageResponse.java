package com.family.finance.common.web;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        long page,
        long pageSize,
        long total,
        long totalPages
) {
    public static <T> PageResponse<T> of(List<T> items, long page, long pageSize, long total) {
        return new PageResponse<>(items, page, pageSize, total,
                total == 0 ? 0 : (total + pageSize - 1) / pageSize);
    }
}
