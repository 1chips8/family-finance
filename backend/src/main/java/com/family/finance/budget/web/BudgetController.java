package com.family.finance.budget.web;

import com.family.finance.budget.dto.BudgetOverviewResponse;
import com.family.finance.budget.dto.BudgetRequest;
import com.family.finance.budget.service.BudgetService;
import com.family.finance.common.error.ApiException;
import com.family.finance.common.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM");
    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) { this.budgetService = budgetService; }

    @GetMapping
    public ApiResponse<BudgetOverviewResponse> overview(@RequestParam(required = false) String month) {
        return ApiResponse.of(budgetService.overview(parseMonth(month, true)));
    }

    @PutMapping("/total")
    public ApiResponse<BudgetOverviewResponse> upsertTotal(@RequestParam(required = false) String month, @Valid @RequestBody BudgetRequest request) {
        return ApiResponse.of(budgetService.upsertTotal(parseMonth(firstNonBlank(month, request.month()), false), request.amount()));
    }

    @PutMapping("/categories/{categoryId}")
    public ApiResponse<BudgetOverviewResponse> upsertCategory(@PathVariable Long categoryId, @RequestParam(required = false) String month,
                                                               @Valid @RequestBody BudgetRequest request) {
        return ApiResponse.of(budgetService.upsertCategory(categoryId, parseMonth(firstNonBlank(month, request.month()), false), request.amount()));
    }

    @DeleteMapping("/total")
    public ApiResponse<Void> deleteTotal(@RequestParam String month) {
        budgetService.deleteTotal(parseMonth(month, false));
        return ApiResponse.of(null);
    }

    @DeleteMapping("/categories/{categoryId}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long categoryId, @RequestParam String month) {
        budgetService.deleteCategory(categoryId, parseMonth(month, false));
        return ApiResponse.of(null);
    }

    private YearMonth parseMonth(String value, boolean defaultCurrent) {
        if ((value == null || value.isBlank()) && defaultCurrent) return YearMonth.now();
        if (value == null || !value.matches("\\d{4}-\\d{2}")) throw invalidMonth();
        try { return YearMonth.parse(value, MONTH_FORMAT); }
        catch (DateTimeParseException exception) { throw invalidMonth(); }
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private ApiException invalidMonth() { return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_MONTH", "月份必须是有效的 YYYY-MM"); }
}
