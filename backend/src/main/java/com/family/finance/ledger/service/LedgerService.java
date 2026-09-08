package com.family.finance.ledger.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.family.finance.audit.service.AuditLogService;
import com.family.finance.auth.domain.AppUser;
import com.family.finance.auth.domain.UserStatus;
import com.family.finance.auth.mapper.AppUserMapper;
import com.family.finance.category.domain.CategoryType;
import com.family.finance.category.domain.FinanceCategory;
import com.family.finance.category.mapper.CategoryMapper;
import com.family.finance.category.service.CategoryService;
import com.family.finance.common.error.ApiException;
import com.family.finance.common.security.CurrentUser;
import com.family.finance.common.security.CurrentUserService;
import com.family.finance.common.web.PageResponse;
import com.family.finance.ledger.domain.LedgerEntry;
import com.family.finance.ledger.domain.LedgerType;
import com.family.finance.ledger.dto.CreateEntryRequest;
import com.family.finance.ledger.dto.EntryPageResponse;
import com.family.finance.ledger.dto.EntryQuery;
import com.family.finance.ledger.dto.EntryResponse;
import com.family.finance.ledger.mapper.LedgerEntryMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 流水业务入口。
 *
 * <p>家庭范围和成员范围都在服务端追加到查询条件中：家长可以查看全家流水，
 * 普通成员只能查看和维护自己的流水，不能依赖前端隐藏按钮来保证权限。</p>
 */
@Service
public class LedgerService {
    private final LedgerEntryMapper entryMapper;
    private final AppUserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final CategoryService categoryService;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public LedgerService(LedgerEntryMapper entryMapper, AppUserMapper userMapper, CategoryMapper categoryMapper,
                         CategoryService categoryService, CurrentUserService currentUserService,
                         AuditLogService auditLogService) {
        this.entryMapper = entryMapper;
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
        this.categoryService = categoryService;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    public EntryPageResponse list(EntryQuery input) {
        CurrentUser user = currentUserService.requireHouseholdUser();
        EntryQuery query = input.normalized();
        Page<LedgerEntry> page = new Page<>(query.page(), query.pageSize());
        QueryWrapper<LedgerEntry> wrapper = new QueryWrapper<>();
        // household_id 是所有流水查询的第一层隔离条件，后续筛选不得绕过它。
        wrapper.eq("household_id", user.householdId());
        if (!user.isParent()) wrapper.eq("member_id", user.id());
        else if (query.memberId() != null) wrapper.eq("member_id", query.memberId());
        if (query.from() != null) wrapper.ge("occurred_on", query.from());
        if (query.to() != null) wrapper.le("occurred_on", query.to());
        if (query.type() != null) wrapper.eq("type", query.type());
        if (query.categoryId() != null) wrapper.eq("category_id", query.categoryId());
        wrapper.orderByDesc("occurred_on", "created_at");
        Page<LedgerEntry> result = entryMapper.selectPage(page, wrapper);
        Map<Long, AppUser> members = memberMap(result.getRecords());
        Map<Long, FinanceCategory> categories = categoryMap(result.getRecords());
        return EntryPageResponse.from(PageResponse.of(result.getRecords().stream()
                        .map(entry -> EntryResponse.from(entry,
                                members.get(entry.getMemberId()).getDisplayName(),
                                members.get(entry.getMemberId()).getMemberNo(),
                                categories.get(entry.getCategoryId()).getName()))
                        .toList(), result.getCurrent(), result.getSize(), result.getTotal()));
    }

    @Transactional
    public EntryResponse create(CreateEntryRequest request) {
        CurrentUser user = currentUserService.requireHouseholdUser();
        EntryResponse response = createImported(user, request);
        auditLogService.record(user, "ENTRY_CREATE", "ENTRY", response.id(),
                "新增" + (response.type() == LedgerType.INCOME ? "收入" : "支出")
                        + "流水 ¥" + response.amount());
        return response;
    }

    public EntryResponse createImported(CurrentUser user, CreateEntryRequest request) {
        AppUser member = resolveMember(request.memberId(), user, true);
        FinanceCategory category = categoryService.requireUsable(request.categoryId(), user.householdId(),
                CategoryType.valueOf(request.type().name()), false);
        validateDate(request.occurredOn());
        LedgerEntry entry = new LedgerEntry();
        entry.setHouseholdId(user.householdId());
        entry.setMemberId(member.getId());
        entry.setCategoryId(category.getId());
        entry.setType(request.type());
        entry.setAmount(request.amount());
        entry.setOccurredOn(request.occurredOn());
        entry.setNote(cleanNote(request.note()));
        entry.setDeleted(false);
        entryMapper.insert(entry);
        return response(entry, member, category);
    }

    @Transactional
    public EntryResponse update(Long id, CreateEntryRequest request) {
        CurrentUser user = currentUserService.requireHouseholdUser();
        LedgerEntry entry = target(id, user);
        AppUser member = resolveMember(request.memberId(), user, true);
        FinanceCategory category = categoryService.requireUsable(request.categoryId(), user.householdId(),
                CategoryType.valueOf(request.type().name()),
                entry.getCategoryId().equals(request.categoryId()));
        validateDate(request.occurredOn());
        entry.setMemberId(member.getId());
        entry.setCategoryId(category.getId());
        entry.setType(request.type());
        entry.setAmount(request.amount());
        entry.setOccurredOn(request.occurredOn());
        entry.setNote(cleanNote(request.note()));
        entryMapper.updateById(entry);
        EntryResponse response = response(entry, member, category);
        auditLogService.record(user, "ENTRY_UPDATE", "ENTRY", entry.getId(),
                "修改流水 ¥" + entry.getAmount());
        return response;
    }

    @Transactional
    public void delete(Long id) {
        CurrentUser user = currentUserService.requireHouseholdUser();
        LedgerEntry entry = target(id, user);
        entryMapper.deleteById(entry.getId());
        auditLogService.record(user, "ENTRY_DELETE", "ENTRY", entry.getId(),
                "删除流水 ¥" + entry.getAmount());
    }

    private LedgerEntry target(Long id, CurrentUser user) {
        LedgerEntry entry = entryMapper.selectById(id);
        // 对不存在和越权访问统一返回 404，避免泄露其他家庭或成员的流水是否存在。
        if (entry == null || !user.householdId().equals(entry.getHouseholdId())
                || (!user.isParent() && !user.id().equals(entry.getMemberId()))) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ENTRY_NOT_FOUND", "流水不存在或无权访问");
        }
        return entry;
    }

    private AppUser resolveMember(Long requestedId, CurrentUser user, boolean activeRequired) {
        // 普通成员提交的 memberId 不可信，强制绑定为当前登录用户。
        Long memberId = user.isParent() ? requestedId : user.id();
        if (memberId == null) throw new ApiException(HttpStatus.BAD_REQUEST, "MEMBER_REQUIRED", "请选择归属成员");
        AppUser member = userMapper.selectById(memberId);
        if (member == null || !user.householdId().equals(member.getHouseholdId())
                || (activeRequired && member.getStatus() != UserStatus.ACTIVE)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MEMBER_INVALID", "归属成员不存在或已停用");
        }
        return member;
    }

    private void validateDate(LocalDate date) {
        if (date.isAfter(LocalDate.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DATE_IN_FUTURE", "发生日期不能晚于今天");
        }
    }

    private String cleanNote(String note) { return note == null ? null : note.trim(); }

    private EntryResponse response(LedgerEntry entry, AppUser member, FinanceCategory category) {
        return EntryResponse.from(entry, member.getDisplayName(), member.getMemberNo(), category.getName());
    }

    private Map<Long, AppUser> memberMap(java.util.List<LedgerEntry> entries) {
        if (entries.isEmpty()) return Collections.emptyMap();
        return userMapper.selectBatchIds(entries.stream().map(LedgerEntry::getMemberId).distinct().toList())
                .stream().collect(Collectors.toMap(AppUser::getId, Function.identity()));
    }

    private Map<Long, FinanceCategory> categoryMap(java.util.List<LedgerEntry> entries) {
        if (entries.isEmpty()) return Collections.emptyMap();
        return categoryMapper.selectBatchIds(entries.stream().map(LedgerEntry::getCategoryId).distinct().toList())
                .stream().collect(Collectors.toMap(FinanceCategory::getId, Function.identity()));
    }
}
