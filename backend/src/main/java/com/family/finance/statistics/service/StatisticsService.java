package com.family.finance.statistics.service;

import com.family.finance.common.error.ApiException;
import com.family.finance.common.security.CurrentUser;
import com.family.finance.common.security.CurrentUserService;
import com.family.finance.budget.dto.BudgetOverviewResponse;
import com.family.finance.budget.dto.BudgetOverviewResponse.BudgetStatus;
import com.family.finance.ledger.domain.LedgerType;
import com.family.finance.ledger.dto.EntryResponse;
import com.family.finance.statistics.dto.DashboardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

@Service
public class StatisticsService {
    private final JdbcTemplate jdbcTemplate;
    private final CurrentUserService currentUserService;

    public StatisticsService(JdbcTemplate jdbcTemplate, CurrentUserService currentUserService) {
        this.jdbcTemplate = jdbcTemplate;
        this.currentUserService = currentUserService;
    }

    public DashboardResponse dashboard(LocalDate from, LocalDate to) {
        CurrentUser user = currentUserService.requireHouseholdUser();
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.withDayOfMonth(1) : from;
        if (start.isAfter(end)) throw new ApiException(HttpStatus.BAD_REQUEST, "DATE_RANGE_INVALID", "开始日期不能晚于结束日期");
        if (end.isAfter(LocalDate.now())) throw new ApiException(HttpStatus.BAD_REQUEST, "DATE_IN_FUTURE", "结束日期不能晚于今天");
        DashboardResponse.Totals totals = totals(user.householdId(), start, end);
        YearMonth lastMonth = YearMonth.from(end);
        YearMonth firstMonth = lastMonth.minusMonths(11);
        long rangeDays = ChronoUnit.DAYS.between(start, end) + 1;
        LocalDate previousTo = start.minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(rangeDays - 1);
        DashboardResponse.Totals previous = totals(user.householdId(), previousFrom, previousTo);
        DashboardResponse.Comparison comparison = comparison(totals, previous);
        BigDecimal savingsRate = totals.income().signum() == 0 ? null
                : totals.balance().divide(totals.income(), 4, RoundingMode.HALF_UP);
        YearMonth reportMonth = YearMonth.from(end);
        return new DashboardResponse(start, end, totals,
                trend(user.householdId(), firstMonth, lastMonth),
                composition(user.householdId(), start, end),
                members(user.householdId(), start, end),
                recent(user, start, end), comparison, savingsRate,
                budget(user.householdId(), reportMonth), anomalies(user.householdId(), reportMonth));
    }

    private DashboardResponse.Comparison comparison(DashboardResponse.Totals current,
                                                     DashboardResponse.Totals previous) {
        return new DashboardResponse.Comparison(
                money(current.income().subtract(previous.income())),
                money(current.expense().subtract(previous.expense())),
                money(current.balance().subtract(previous.balance())),
                previous,
                changeRate(current.income(), previous.income()),
                changeRate(current.expense(), previous.expense()),
                changeRate(current.balance(), previous.balance()));
    }

    private BigDecimal changeRate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) return null;
        return current.subtract(previous).divide(previous.abs(), 4, RoundingMode.HALF_UP);
    }

    private BudgetOverviewResponse budget(Long householdId, YearMonth month) {
        List<BudgetRow> rows = jdbcTemplate.query("""
                SELECT b.category_id, b.amount, c.name
                FROM monthly_budget b LEFT JOIN finance_category c ON c.id=b.category_id
                WHERE b.household_id=? AND b.budget_month=? ORDER BY b.category_id
                """, (rs, rowNum) -> new BudgetRow(
                rs.getObject(1, Long.class), money(rs.getBigDecimal(2)), rs.getString(3)), householdId, month.atDay(1));
        if (rows == null) rows = List.of();
        BigDecimal configured = rows.stream().filter(row -> row.categoryId() == null)
                .map(BudgetRow::amount).findFirst().orElse(money(BigDecimal.ZERO));
        BigDecimal spent = expense(householdId, month.atDay(1), month.atEndOfMonth(), null);
        List<BudgetOverviewResponse.BudgetItem> categories = rows.stream().filter(row -> row.categoryId() != null)
                .map(row -> {
                    BigDecimal categorySpent = expense(householdId, month.atDay(1), month.atEndOfMonth(), row.categoryId());
                    return new BudgetOverviewResponse.BudgetItem(null, row.categoryId(), row.name(), row.amount(), categorySpent,
                            money(row.amount().subtract(categorySpent)), rate(categorySpent, row.amount()),
                            status(categorySpent, row.amount()));
                }).toList();
        BudgetOverviewResponse.BudgetItem total = rows.stream().filter(row -> row.categoryId() == null)
                .findFirst().map(row -> new BudgetOverviewResponse.BudgetItem(null, null, null, row.amount(), spent,
                        money(row.amount().subtract(spent)), rate(spent, row.amount()), status(spent, row.amount())))
                .orElse(null);
        return new BudgetOverviewResponse(month, total, categories);
    }

    private List<DashboardResponse.Anomaly> anomalies(Long householdId, YearMonth month) {
        LocalDate currentFrom = month.atDay(1), currentTo = month.atEndOfMonth();
        LocalDate historyFrom = month.minusMonths(3).atDay(1), historyTo = month.minusMonths(1).atEndOfMonth();
        List<CurrentCategory> current = jdbcTemplate.query("""
                SELECT e.category_id, c.name, SUM(e.amount)
                FROM ledger_entry e JOIN finance_category c ON c.id=e.category_id
                WHERE e.household_id=? AND e.deleted=0 AND e.type='EXPENSE'
                  AND e.occurred_on BETWEEN ? AND ? GROUP BY e.category_id, c.name
                """, (rs, rowNum) -> new CurrentCategory(rs.getLong(1), rs.getString(2), money(rs.getBigDecimal(3))),
                householdId, currentFrom, currentTo);
        List<HistoryCategory> history = jdbcTemplate.query("""
                SELECT e.category_id, SUM(e.amount), COUNT(DISTINCT DATE_FORMAT(e.occurred_on, '%Y-%m'))
                FROM ledger_entry e
                WHERE e.household_id=? AND e.deleted=0 AND e.type='EXPENSE'
                  AND e.occurred_on BETWEEN ? AND ? GROUP BY e.category_id
                """, (rs, rowNum) -> new HistoryCategory(rs.getLong(1), money(rs.getBigDecimal(2)), rs.getInt(3)),
                householdId, historyFrom, historyTo);
        if (current == null || history == null) return List.of();
        Map<Long, HistoryCategory> byCategory = new HashMap<>();
        history.forEach(item -> byCategory.put(item.categoryId(), item));
        List<DashboardResponse.Anomaly> result = new ArrayList<>();
        for (CurrentCategory item : current) {
            HistoryCategory baseline = byCategory.get(item.categoryId());
            if (item.amount().compareTo(new BigDecimal("100.00")) < 0 || baseline == null || baseline.months() < 3) continue;
            BigDecimal average = baseline.total().divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
            if (average.signum() == 0 || item.amount().compareTo(average.multiply(new BigDecimal("1.5"))) <= 0) continue;
            result.add(new DashboardResponse.Anomaly(item.categoryId(), item.name(), item.amount(), average,
                    item.amount().divide(average, 4, RoundingMode.HALF_UP)));
        }
        return result;
    }

    private BigDecimal expense(Long householdId, LocalDate from, LocalDate to, Long categoryId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM ledger_entry WHERE household_id=? AND deleted=0 "
                + "AND type='EXPENSE' AND occurred_on BETWEEN ? AND ?" + (categoryId == null ? "" : " AND category_id=?");
        Object[] args = categoryId == null ? new Object[]{householdId, from, to} : new Object[]{householdId, from, to, categoryId};
        return money(jdbcTemplate.queryForObject(sql, BigDecimal.class, args));
    }

    private BigDecimal rate(BigDecimal spent, BigDecimal budget) {
        return budget.signum() == 0 ? null : spent.divide(budget, 4, RoundingMode.HALF_UP);
    }

    private BudgetStatus status(BigDecimal spent, BigDecimal budget) {
        if (budget.signum() == 0) return BudgetStatus.NORMAL;
        BigDecimal ratio = spent.divide(budget, 6, RoundingMode.HALF_UP);
        return ratio.compareTo(BigDecimal.ONE) > 0 ? BudgetStatus.OVER
                : ratio.compareTo(new BigDecimal("0.80")) >= 0 ? BudgetStatus.WARNING : BudgetStatus.NORMAL;
    }

    private record BudgetRow(Long categoryId, BigDecimal amount, String name) {}
    private record CurrentCategory(Long categoryId, String name, BigDecimal amount) {}
    private record HistoryCategory(Long categoryId, BigDecimal total, int months) {}

    private DashboardResponse.Totals totals(Long householdId, LocalDate from, LocalDate to) {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(CASE WHEN type='INCOME' THEN amount ELSE 0 END), 0),
                       COALESCE(SUM(CASE WHEN type='EXPENSE' THEN amount ELSE 0 END), 0)
                FROM ledger_entry WHERE household_id=? AND deleted=0 AND occurred_on BETWEEN ? AND ?
                """, (rs, rowNum) -> {
            BigDecimal income = money(rs.getBigDecimal(1));
            BigDecimal expense = money(rs.getBigDecimal(2));
            return new DashboardResponse.Totals(income, expense, money(income.subtract(expense)));
        }, householdId, from, to);
    }

    private List<DashboardResponse.TrendPoint> trend(Long householdId, YearMonth from, YearMonth to) {
        return java.util.stream.Stream.iterate(from, month -> !month.isAfter(to), month -> month.plusMonths(1))
                .map(month -> {
                    LocalDate start = month.atDay(1), end = month.atEndOfMonth();
                    DashboardResponse.Totals totals = totals(householdId, start, end);
                    return new DashboardResponse.TrendPoint(month.toString(), totals.income(), totals.expense());
                }).toList();
    }

    private List<DashboardResponse.CategoryBreakdown> composition(Long householdId, LocalDate from, LocalDate to) {
        return jdbcTemplate.query("""
                SELECT c.id, c.name, e.type, COALESCE(SUM(e.amount), 0)
                FROM ledger_entry e JOIN finance_category c ON c.id=e.category_id
                WHERE e.household_id=? AND e.deleted=0 AND e.occurred_on BETWEEN ? AND ?
                GROUP BY c.id, c.name, e.type ORDER BY e.type, SUM(e.amount) DESC
                """, (rs, rowNum) -> new DashboardResponse.CategoryBreakdown(rs.getLong(1), rs.getString(2),
                        rs.getString(3), money(rs.getBigDecimal(4))), householdId, from, to);
    }

    private List<DashboardResponse.MemberComparison> members(Long householdId, LocalDate from, LocalDate to) {
        return jdbcTemplate.query("""
                SELECT u.id, u.member_no, u.display_name,
                       COALESCE(SUM(CASE WHEN e.type='INCOME' THEN e.amount ELSE 0 END), 0),
                       COALESCE(SUM(CASE WHEN e.type='EXPENSE' THEN e.amount ELSE 0 END), 0)
                FROM app_user u LEFT JOIN ledger_entry e ON e.member_id=u.id AND e.household_id=?
                    AND e.deleted=0 AND e.occurred_on BETWEEN ? AND ?
                WHERE u.household_id=? GROUP BY u.id, u.member_no, u.display_name ORDER BY u.member_no
                """, (rs, rowNum) -> {
            BigDecimal income = money(rs.getBigDecimal(4)), expense = money(rs.getBigDecimal(5));
            return new DashboardResponse.MemberComparison(rs.getLong(1), rs.getString(2), rs.getString(3),
                    income, expense, money(income.subtract(expense)));
        }, householdId, from, to, householdId);
    }

    private List<EntryResponse> recent(CurrentUser user, LocalDate from, LocalDate to) {
        String sql = """
                SELECT e.id, e.member_id, u.display_name, u.member_no, e.type, e.category_id, c.name,
                       e.amount, e.occurred_on, e.note, e.created_at
                FROM ledger_entry e JOIN app_user u ON u.id=e.member_id
                JOIN finance_category c ON c.id=e.category_id
                WHERE e.household_id=? AND e.deleted=0 AND e.occurred_on BETWEEN ? AND ?
                """ + (!user.isParent() ? " AND e.member_id=? " : "") +
                " ORDER BY e.occurred_on DESC, e.created_at DESC LIMIT 8";
        Object[] args = user.isParent() ? new Object[]{user.householdId(), from, to} : new Object[]{user.householdId(), from, to, user.id()};
        return jdbcTemplate.query(sql, (rs, rowNum) -> new EntryResponse(rs.getLong(1), rs.getLong(2), rs.getString(3),
                rs.getString(4), LedgerType.valueOf(rs.getString(5)), rs.getLong(6), rs.getString(7), money(rs.getBigDecimal(8)),
                rs.getObject(9, LocalDate.class), rs.getString(10), rs.getTimestamp(11).toLocalDateTime()), args);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
