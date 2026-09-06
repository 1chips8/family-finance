package com.family.finance.budget.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.family.finance.budget.domain.MonthlyBudget;
import com.family.finance.budget.dto.BudgetOverviewResponse;
import com.family.finance.budget.dto.BudgetOverviewResponse.BudgetStatus;
import com.family.finance.budget.mapper.MonthlyBudgetMapper;
import com.family.finance.audit.service.AuditLogService;
import com.family.finance.category.domain.CategoryType;
import com.family.finance.category.domain.FinanceCategory;
import com.family.finance.category.service.CategoryService;
import com.family.finance.common.error.ApiException;
import com.family.finance.common.security.CurrentUser;
import com.family.finance.common.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

@Service
public class BudgetService {
    private static final int MONEY_SCALE = 2;

    private final MonthlyBudgetMapper budgetMapper;
    private final CategoryService categoryService;
    private final CurrentUserService currentUserService;
    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;

    @Autowired
    public BudgetService(MonthlyBudgetMapper budgetMapper, CategoryService categoryService,
                         CurrentUserService currentUserService, JdbcTemplate jdbcTemplate,
                         AuditLogService auditLogService) {
        this.budgetMapper = budgetMapper;
        this.categoryService = categoryService;
        this.currentUserService = currentUserService;
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogService = auditLogService;
    }

    /** Constructor kept for focused service tests and lightweight callers. */
    public BudgetService(MonthlyBudgetMapper budgetMapper, CategoryService categoryService,
                         CurrentUserService currentUserService, JdbcTemplate jdbcTemplate) {
        this(budgetMapper, categoryService, currentUserService, jdbcTemplate, null);
    }

    public BudgetOverviewResponse overview(YearMonth month) {
        CurrentUser user = currentUserService.requireHouseholdUser();
        return overview(user, month);
    }

    private BudgetOverviewResponse overview(CurrentUser user, YearMonth month) {
        YearMonth selected = requireMonth(month);
        List<MonthlyBudget> budgets = budgetMapper.selectList(new QueryWrapper<MonthlyBudget>()
                .eq("household_id", user.householdId())
                .eq("budget_month", selected.atDay(1))
                .orderByAsc("category_id"));
        if (budgets == null) budgets = List.of();

        MonthlyBudget total = budgets.stream().filter(item -> item.getCategoryId() == null).findFirst().orElse(null);
        BigDecimal totalBudget = total == null ? money(BigDecimal.ZERO) : money(total.getAmount());
        BigDecimal totalSpent = spent(user.householdId(), selected.atDay(1), selected.atEndOfMonth(), null);
        List<BudgetOverviewResponse.BudgetItem> categories = budgets.stream()
                .filter(item -> item.getCategoryId() != null)
                .map(item -> categoryBudget(user.householdId(), selected, item))
                .sorted(Comparator.comparing(BudgetOverviewResponse.BudgetItem::status,
                        Comparator.comparingInt(BudgetStatus::ordinal).reversed())
                        .thenComparing(BudgetOverviewResponse.BudgetItem::categoryName,
                                Comparator.nullsLast(String::compareTo)))
                .toList();
        BudgetOverviewResponse.BudgetItem totalItem = total == null ? null
                : item(total.getId(), null, null, totalBudget, totalSpent);
        return new BudgetOverviewResponse(selected, totalItem, categories);
    }

    @Transactional
    public BudgetOverviewResponse upsertTotal(YearMonth month, BigDecimal amount) {
        CurrentUser user = currentUserService.requireParent();
        MonthlyBudget budget = find(user.householdId(), requireMonth(month), null);
        MonthlyBudget saved = save(user.householdId(), requireMonth(month), null, amount, budget);
        record(user, "UPSERT", saved.getId(), "设置家庭总预算 ¥" + saved.getAmount());
        return overview(user, month);
    }

    @Transactional
    public BudgetOverviewResponse upsertCategory(Long categoryId, YearMonth month, BigDecimal amount) {
        CurrentUser user = currentUserService.requireParent();
        if (categoryId == null) throw invalid("CATEGORY_REQUIRED", "分类不能为空");
        categoryService.requireUsable(categoryId, user.householdId(), CategoryType.EXPENSE, false);
        MonthlyBudget budget = find(user.householdId(), requireMonth(month), categoryId);
        MonthlyBudget saved = save(user.householdId(), requireMonth(month), categoryId, amount, budget);
        record(user, "UPSERT", saved.getId(), "设置分类预算 ¥" + saved.getAmount());
        return overview(user, month);
    }

    @Transactional
    public void deleteTotal(YearMonth month) {
        delete(month, null);
    }

    @Transactional
    public void deleteCategory(Long categoryId, YearMonth month) {
        if (categoryId == null) throw invalid("CATEGORY_REQUIRED", "分类不能为空");
        delete(month, categoryId);
    }

    private void delete(YearMonth month, Long categoryId) {
        CurrentUser user = currentUserService.requireParent();
        MonthlyBudget budget = find(user.householdId(), requireMonth(month), categoryId);
        if (budget != null) {
            budgetMapper.deleteById(budget.getId());
            record(user, "DELETE", budget.getId(), categoryId == null ? "删除家庭总预算" : "删除分类预算");
        }
    }

    private BudgetOverviewResponse.BudgetItem categoryBudget(Long householdId, YearMonth month, MonthlyBudget budget) {
        FinanceCategory category = categoryService.requireUsable(budget.getCategoryId(), householdId, CategoryType.EXPENSE, true);
        BigDecimal configured = money(budget.getAmount());
        BigDecimal spent = spent(householdId, month.atDay(1), month.atEndOfMonth(), budget.getCategoryId());
        return item(budget.getId(), budget.getCategoryId(), category.getName(), configured, spent);
    }

    private BudgetOverviewResponse.BudgetItem item(Long id, Long categoryId, String name, BigDecimal budget,
                                                    BigDecimal spent) {
        return new BudgetOverviewResponse.BudgetItem(id, categoryId, name, budget, spent,
                money(budget.subtract(spent)), rate(spent, budget), status(spent, budget));
    }

    private MonthlyBudget find(Long householdId, YearMonth month, Long categoryId) {
        QueryWrapper<MonthlyBudget> query = new QueryWrapper<MonthlyBudget>()
                .eq("household_id", householdId).eq("budget_month", month.atDay(1));
        if (categoryId == null) query.isNull("category_id"); else query.eq("category_id", categoryId);
        return budgetMapper.selectOne(query);
    }

    private MonthlyBudget save(Long householdId, YearMonth month, Long categoryId, BigDecimal amount, MonthlyBudget existing) {
        BigDecimal value = normalize(amount);
        if (existing == null) {
            existing = new MonthlyBudget();
            existing.setHouseholdId(householdId);
            existing.setBudgetMonth(month.atDay(1));
            existing.setCategoryId(categoryId);
            existing.setAmount(value);
            budgetMapper.insert(existing);
        } else {
            existing.setAmount(value);
            budgetMapper.updateById(existing);
        }
        return existing;
    }

    private void record(CurrentUser actor, String action, Long objectId, String summary) {
        if (auditLogService != null) auditLogService.record(actor, action, "MONTHLY_BUDGET", objectId, summary);
    }

    private BigDecimal spent(Long householdId, LocalDate from, LocalDate to, Long categoryId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM ledger_entry "
                + "WHERE household_id=? AND deleted=0 AND type='EXPENSE' AND occurred_on BETWEEN ? AND ?"
                + (categoryId == null ? "" : " AND category_id=?");
        Object[] args = categoryId == null ? new Object[]{householdId, from, to} : new Object[]{householdId, from, to, categoryId};
        BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
        return money(result);
    }

    private BigDecimal normalize(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw invalid("AMOUNT_INVALID", "预算金额必须大于0");
        if (amount.scale() > MONEY_SCALE) throw invalid("AMOUNT_INVALID", "预算金额最多两位小数");
        return amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) { return (value == null ? BigDecimal.ZERO : value).setScale(MONEY_SCALE, RoundingMode.HALF_UP); }

    private BigDecimal rate(BigDecimal spent, BigDecimal budget) {
        return budget.signum() == 0 ? null : spent.divide(budget, 4, RoundingMode.HALF_UP);
    }

    private BudgetStatus status(BigDecimal spent, BigDecimal budget) {
        if (budget.signum() == 0) return BudgetStatus.NORMAL;
        BigDecimal ratio = spent.divide(budget, 6, RoundingMode.HALF_UP);
        return ratio.compareTo(BigDecimal.ONE) > 0 ? BudgetStatus.OVER
                : ratio.compareTo(new BigDecimal("0.80")) >= 0 ? BudgetStatus.WARNING : BudgetStatus.NORMAL;
    }

    private YearMonth requireMonth(YearMonth month) {
        if (month == null) throw invalid("INVALID_MONTH", "月份不能为空");
        return month;
    }

    private ApiException invalid(String code, String message) { return new ApiException(HttpStatus.BAD_REQUEST, code, message); }
}
