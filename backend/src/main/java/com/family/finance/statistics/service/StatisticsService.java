package com.family.finance.statistics.service;

import com.family.finance.common.error.ApiException;
import com.family.finance.common.security.CurrentUser;
import com.family.finance.common.security.CurrentUserService;
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
        return new DashboardResponse(start, end, totals,
                trend(user.householdId(), firstMonth, lastMonth),
                composition(user.householdId(), start, end),
                members(user.householdId(), start, end),
                recent(user, start, end));
    }

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
