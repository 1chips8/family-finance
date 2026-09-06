package com.family.finance.category.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.family.finance.category.domain.*;
import com.family.finance.category.dto.*;
import com.family.finance.category.mapper.CategoryMapper;
import com.family.finance.audit.service.AuditLogService;
import com.family.finance.common.error.ApiException;
import com.family.finance.common.security.CurrentUser;
import com.family.finance.common.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class CategoryService {
    private final CategoryMapper categoryMapper;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public CategoryService(CategoryMapper categoryMapper, CurrentUserService currentUserService,
                           AuditLogService auditLogService) {
        this.categoryMapper = categoryMapper;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    public List<CategoryResponse> list(boolean includeInactive) {
        CurrentUser user = currentUserService.requireHouseholdUser();
        QueryWrapper<FinanceCategory> wrapper = new QueryWrapper<>();
        wrapper.and(w -> w.isNull("household_id").or().eq("household_id", user.householdId()));
        if (!includeInactive) wrapper.eq("status", CategoryStatus.ACTIVE.name());
        return categoryMapper.selectList(wrapper).stream()
                .sorted(Comparator.comparing(FinanceCategory::getType).thenComparing(FinanceCategory::getScope).thenComparing(FinanceCategory::getName))
                .map(CategoryResponse::from).toList();
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        CurrentUser user = currentUserService.requireParent();
        FinanceCategory category = new FinanceCategory();
        category.setHouseholdId(user.householdId());
        category.setScope(CategoryScope.CUSTOM);
        category.setType(request.type());
        category.setName(request.name().trim());
        category.setStatus(CategoryStatus.ACTIVE);
        categoryMapper.insert(category);
        auditLogService.record(user, "CATEGORY_CREATE", "CATEGORY", category.getId(),
                "新增" + (category.getType() == CategoryType.INCOME ? "收入" : "支出") + "分类 " + category.getName());
        return CategoryResponse.from(category);
    }

    @Transactional
    public CategoryResponse update(Long id, UpdateCategoryRequest request) {
        CurrentUser user = currentUserService.requireParent();
        FinanceCategory category = customCategory(id, user.householdId());
        category.setName(request.name().trim());
        categoryMapper.updateById(category);
        auditLogService.record(user, "CATEGORY_UPDATE", "CATEGORY", category.getId(),
                "修改分类为 " + category.getName());
        return CategoryResponse.from(category);
    }

    @Transactional
    public CategoryResponse updateStatus(Long id, UpdateCategoryStatusRequest request) {
        CurrentUser user = currentUserService.requireParent();
        FinanceCategory category = customCategory(id, user.householdId());
        category.setStatus(request.active() ? CategoryStatus.ACTIVE : CategoryStatus.INACTIVE);
        categoryMapper.updateById(category);
        auditLogService.record(user, "CATEGORY_STATUS", "CATEGORY", category.getId(),
                (request.active() ? "恢复分类 " : "停用分类 ") + category.getName());
        return CategoryResponse.from(category);
    }

    public FinanceCategory requireUsable(Long id, Long householdId, CategoryType type, boolean allowInactive) {
        FinanceCategory category = categoryMapper.selectById(id);
        if (category == null || (category.getHouseholdId() != null && !householdId.equals(category.getHouseholdId()))
                || category.getType() != type
                || (!allowInactive && category.getStatus() != CategoryStatus.ACTIVE)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CATEGORY_INVALID", "分类不存在、已停用或与流水类型不匹配");
        }
        return category;
    }

    private FinanceCategory customCategory(Long id, Long householdId) {
        FinanceCategory category = categoryMapper.selectById(id);
        if (category == null || category.getScope() != CategoryScope.CUSTOM
                || !householdId.equals(category.getHouseholdId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "自定义分类不存在");
        }
        return category;
    }
}
