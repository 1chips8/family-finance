package com.family.finance.ledger.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.family.finance.audit.service.AuditLogService;
import com.family.finance.auth.domain.AppUser;
import com.family.finance.auth.domain.UserStatus;
import com.family.finance.auth.mapper.AppUserMapper;
import com.family.finance.category.domain.CategoryStatus;
import com.family.finance.category.domain.CategoryType;
import com.family.finance.category.domain.FinanceCategory;
import com.family.finance.category.mapper.CategoryMapper;
import com.family.finance.common.error.ApiException;
import com.family.finance.common.security.CurrentUser;
import com.family.finance.common.security.CurrentUserService;
import com.family.finance.ledger.csv.CsvCodec;
import com.family.finance.ledger.domain.LedgerType;
import com.family.finance.ledger.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * 流水 CSV 导入导出服务。
 *
 * <p>导入采用“预览—确认”两阶段流程：两次都执行完整解析和权限校验，
 * 并用摘要确认用户提交的仍是刚刚预览过的内容。</p>
 */
@Service
public class EntryCsvService {
    private static final List<String> HEADER = List.of("发生日期", "类型", "分类", "成员编号", "金额", "备注");
    private static final int MAX_ROWS = 500;
    private final CurrentUserService currentUserService;
    private final AppUserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final LedgerService ledgerService;
    private final AuditLogService auditLogService;

    public EntryCsvService(CurrentUserService currentUserService, AppUserMapper userMapper,
                           CategoryMapper categoryMapper, LedgerService ledgerService,
                           AuditLogService auditLogService) {
        this.currentUserService = currentUserService;
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
        this.ledgerService = ledgerService;
        this.auditLogService = auditLogService;
    }

    public EntryImportPreviewResponse preview(String csv) {
        CurrentUser user = currentUserService.requireHouseholdUser();
        return parseAndValidate(csv, user);
    }

    @Transactional
    public EntryImportCommitResponse commit(EntryImportRequest request) {
        CurrentUser user = currentUserService.requireHouseholdUser();
        EntryImportPreviewResponse preview = parseAndValidate(request.csv(), user);
        // 防止预览后文件被替换，避免用户确认的内容与最终写入内容不一致。
        if (!MessageDigest.isEqual(preview.checksum().getBytes(StandardCharsets.UTF_8),
                request.checksum().getBytes(StandardCharsets.UTF_8))) {
            throw new ApiException(HttpStatus.CONFLICT, "CSV_CHANGED", "CSV 内容已变化，请重新预览");
        }
        if (preview.errorRows() > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CSV_HAS_ERRORS", "请修正全部错误后再导入");
        }
        for (EntryImportPreviewResponse.Row row : preview.rows()) {
            ledgerService.createImported(user, new CreateEntryRequest(row.memberId(), row.type(), row.categoryId(),
                    row.amount(), row.occurredOn(), row.note()));
        }
        auditLogService.record(user, "ENTRY_IMPORT", "ENTRY", null,
                "批量导入 " + preview.validRows() + " 笔流水");
        return new EntryImportCommitResponse(preview.validRows());
    }

    public byte[] export(EntryQuery query) {
        currentUserService.requireHouseholdUser();
        StringBuilder csv = new StringBuilder("\uFEFF").append(CsvCodec.line(HEADER));
        // 分页读取，避免一次导出把全部流水同时加载进内存。
        long page = 1;
        while (true) {
            EntryPageResponse result = ledgerService.list(new EntryQuery(query.from(), query.to(), query.type(),
                    query.categoryId(), query.memberId(), page, 100));
            for (EntryResponse entry : result.items()) {
                csv.append(CsvCodec.line(List.of(entry.occurredOn().toString(),
                        entry.type() == LedgerType.INCOME ? "收入" : "支出",
                        CsvCodec.spreadsheetSafe(entry.categoryName()), entry.memberNo(),
                        entry.amount().setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
                        CsvCodec.spreadsheetSafe(entry.note()))));
            }
            if (page >= result.totalPages()) break;
            page++;
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private EntryImportPreviewResponse parseAndValidate(String csv, CurrentUser user) {
        List<List<String>> records = CsvCodec.parse(csv);
        if (records.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "CSV_EMPTY", "CSV 内容为空");
        List<String> header = records.get(0).stream().map(String::strip).toList();
        if (!header.equals(HEADER)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CSV_HEADER_INVALID",
                    "CSV 列名必须为：" + String.join("、", HEADER));
        }
        if (records.size() - 1 > MAX_ROWS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CSV_TOO_LARGE", "单次最多导入 500 行");
        }
        List<AppUser> members = userMapper.selectList(new QueryWrapper<AppUser>()
                .eq("household_id", user.householdId()).eq("status", UserStatus.ACTIVE));
        // 系统公共分类（household_id 为空）与当前家庭自定义分类都可以用于导入。
        List<FinanceCategory> categories = categoryMapper.selectList(new QueryWrapper<FinanceCategory>()
                .and(w -> w.isNull("household_id").or().eq("household_id", user.householdId()))
                .eq("status", CategoryStatus.ACTIVE));
        List<EntryImportPreviewResponse.Row> rows = new ArrayList<>();
        for (int index = 1; index < records.size(); index++) {
            rows.add(validateRow(records.get(index), index + 1, user, members, categories));
        }
        int valid = (int) rows.stream().filter(EntryImportPreviewResponse.Row::valid).count();
        return new EntryImportPreviewResponse(checksum(csv), rows.size(), valid, rows.size() - valid, List.copyOf(rows));
    }

    private EntryImportPreviewResponse.Row validateRow(List<String> values, int rowNumber, CurrentUser user,
                                                        List<AppUser> members, List<FinanceCategory> categories) {
        Map<String, String> errors = new LinkedHashMap<>();
        List<String> row = new ArrayList<>(values);
        while (row.size() < HEADER.size()) row.add("");
        if (row.size() != HEADER.size()) errors.put("row", "列数必须为 6");
        LocalDate date = parseDate(value(row, 0), errors);
        LedgerType type = parseType(value(row, 1), errors);
        String categoryName = value(row, 2).strip();
        List<FinanceCategory> matchingCategories = categories.stream()
                .filter(item -> type != null && item.getType() == CategoryType.valueOf(type.name()))
                .filter(item -> item.getName().equals(categoryName)).toList();
        FinanceCategory category = matchingCategories.size() == 1 ? matchingCategories.get(0) : null;
        if (matchingCategories.isEmpty()) errors.put("category", "找不到同类型的启用分类");
        else if (matchingCategories.size() > 1) errors.put("category", "存在同名分类，请先将分类名称改为唯一名称");
        String requestedMemberNo = value(row, 3).strip().toUpperCase();
        // 普通成员只能解析到自己；CSV 中伪造其他成员编号不会改变归属人。
        AppUser member = user.isParent()
                ? members.stream().filter(item -> Objects.equals(item.getMemberNo(), requestedMemberNo)).findFirst().orElse(null)
                : members.stream().filter(item -> Objects.equals(item.getId(), user.id())).findFirst().orElse(null);
        if (member == null) errors.put("memberNo", "成员不存在或已停用");
        else if (!user.isParent() && !Objects.equals(member.getMemberNo(), requestedMemberNo)) {
            errors.put("memberNo", "普通成员只能导入自己的成员编号");
        }
        BigDecimal amount = parseAmount(value(row, 4), errors);
        String note = value(row, 5).strip();
        if (note.length() > 255) errors.put("note", "备注不能超过255个字符");
        return new EntryImportPreviewResponse.Row(rowNumber, date, type,
                category == null ? null : category.getId(), categoryName,
                member == null ? null : member.getId(), member == null ? requestedMemberNo : member.getMemberNo(),
                amount, note.isBlank() ? null : note, Map.copyOf(errors));
    }

    private LocalDate parseDate(String raw, Map<String, String> errors) {
        try {
            LocalDate value = LocalDate.parse(raw.strip());
            if (value.isAfter(LocalDate.now())) errors.put("occurredOn", "发生日期不能晚于今天");
            return value;
        } catch (DateTimeParseException exception) {
            errors.put("occurredOn", "日期格式必须为 YYYY-MM-DD"); return null;
        }
    }

    private LedgerType parseType(String raw, Map<String, String> errors) {
        String value = raw.strip().toUpperCase();
        if (value.equals("收入")) return LedgerType.INCOME;
        if (value.equals("支出")) return LedgerType.EXPENSE;
        try { return LedgerType.valueOf(value); }
        catch (IllegalArgumentException exception) { errors.put("type", "类型必须为收入或支出"); return null; }
    }

    private BigDecimal parseAmount(String raw, Map<String, String> errors) {
        try {
            BigDecimal value = new BigDecimal(raw.strip());
            if (value.signum() <= 0 || value.scale() > 2 || value.precision() - value.scale() > 10) {
                errors.put("amount", "金额必须大于0，最多10位整数和2位小数");
            }
            return value;
        } catch (NumberFormatException exception) {
            errors.put("amount", "金额格式不正确"); return null;
        }
    }

    private String value(List<String> values, int index) { return index < values.size() ? values.get(index) : ""; }

    private String checksum(String csv) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(csv.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
