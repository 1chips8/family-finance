package com.family.finance.budget;

import com.family.finance.budget.domain.MonthlyBudget;
import com.family.finance.budget.dto.BudgetOverviewResponse;
import com.family.finance.budget.mapper.MonthlyBudgetMapper;
import com.family.finance.budget.service.BudgetService;
import com.family.finance.category.domain.CategoryStatus;
import com.family.finance.category.domain.CategoryType;
import com.family.finance.category.domain.FinanceCategory;
import com.family.finance.category.service.CategoryService;
import com.family.finance.common.error.ApiException;
import com.family.finance.common.security.CurrentUser;
import com.family.finance.common.security.CurrentUserService;
import com.family.finance.auth.domain.Role;
import com.family.finance.auth.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {
    @Mock MonthlyBudgetMapper budgetMapper;
    @Mock CategoryService categoryService;
    @Mock CurrentUserService currentUserService;
    @Mock JdbcTemplate jdbcTemplate;
    @InjectMocks BudgetService budgetService;

    private final CurrentUser parent = new CurrentUser(1L, "parent", "家长", 9L, "M001", Role.PARENT, UserStatus.ACTIVE);
    private final CurrentUser member = new CurrentUser(2L, "member", "成员", 9L, "M002", Role.MEMBER, UserStatus.ACTIVE);

    @Test
    void memberCannotWriteBudget() {
        when(currentUserService.requireParent()).thenThrow(new ApiException(org.springframework.http.HttpStatus.FORBIDDEN,
                "PARENT_REQUIRED", "仅家长可以执行此操作"));

        assertThatThrownBy(() -> budgetService.upsertTotal(YearMonth.of(2026, 9), new BigDecimal("100")))
                .isInstanceOf(ApiException.class)
                .hasMessage("仅家长可以执行此操作");
        verifyNoInteractions(budgetMapper);
    }

    @Test
    void categoryBudgetRejectsCategoryFromAnotherHousehold() {
        when(currentUserService.requireParent()).thenReturn(parent);
        doThrow(new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "CATEGORY_INVALID", "分类无效"))
                .when(categoryService).requireUsable(77L, 9L, CategoryType.EXPENSE, false);

        assertThatThrownBy(() -> budgetService.upsertCategory(77L, YearMonth.of(2026, 9), new BigDecimal("100")))
                .isInstanceOf(ApiException.class).hasMessage("分类无效");
        verify(budgetMapper, never()).insert(any(MonthlyBudget.class));
    }

    @Test
    void overviewCalculatesSpentRemainingAndWarningStatus() {
        when(currentUserService.requireHouseholdUser()).thenReturn(parent);
        MonthlyBudget total = budget(10L, null, "100.00");
        MonthlyBudget categoryBudget = budget(11L, 7L, "50.00");
        when(budgetMapper.selectList(any())).thenReturn(List.of(total, categoryBudget));
        FinanceCategory category = new FinanceCategory();
        category.setId(7L); category.setName("餐饮"); category.setType(CategoryType.EXPENSE); category.setStatus(CategoryStatus.ACTIVE);
        when(categoryService.requireUsable(7L, 9L, CategoryType.EXPENSE, true)).thenReturn(category);
        when(jdbcTemplate.queryForObject(anyString(), eq(BigDecimal.class), any(Object[].class)))
                .thenReturn(new BigDecimal("80.00"));

        BudgetOverviewResponse response = budgetService.overview(YearMonth.of(2026, 9));

        assertThat(response.total().spent()).isEqualByComparingTo("80.00");
        assertThat(response.total().remaining()).isEqualByComparingTo("20.00");
        assertThat(response.total().status()).isEqualTo(BudgetOverviewResponse.BudgetStatus.WARNING);
        assertThat(response.categories()).hasSize(1);
        assertThat(response.categories().get(0).status()).isEqualTo(BudgetOverviewResponse.BudgetStatus.OVER);
    }

    private MonthlyBudget budget(Long id, Long categoryId, String amount) {
        MonthlyBudget budget = new MonthlyBudget();
        budget.setId(id); budget.setHouseholdId(9L); budget.setBudgetMonth(YearMonth.of(2026, 9).atDay(1));
        budget.setCategoryId(categoryId); budget.setAmount(new BigDecimal(amount));
        return budget;
    }
}
