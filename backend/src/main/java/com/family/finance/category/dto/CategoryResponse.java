package com.family.finance.category.dto;

import com.family.finance.category.domain.CategoryScope;
import com.family.finance.category.domain.CategoryStatus;
import com.family.finance.category.domain.CategoryType;
import com.family.finance.category.domain.FinanceCategory;

public record CategoryResponse(Long id, CategoryScope scope, CategoryType type, String name, CategoryStatus status) {
    public static CategoryResponse from(FinanceCategory category) {
        return new CategoryResponse(category.getId(), category.getScope(), category.getType(), category.getName(), category.getStatus());
    }
}
