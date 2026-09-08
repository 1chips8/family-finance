package com.family.finance.recurring.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import com.family.finance.ledger.domain.LedgerEntry;
import com.family.finance.ledger.mapper.LedgerEntryMapper;
import com.family.finance.recurring.domain.RecurringGeneration;
import com.family.finance.recurring.domain.RecurringStatus;
import com.family.finance.recurring.domain.RecurringTemplate;
import com.family.finance.recurring.dto.RecurringGenerationResponse;
import com.family.finance.recurring.dto.RecurringTemplateRequest;
import com.family.finance.recurring.dto.RecurringTemplateResponse;
import com.family.finance.recurring.mapper.RecurringGenerationMapper;
import com.family.finance.recurring.mapper.RecurringTemplateMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 周期流水模板及手动生成服务。
 *
 * <p>每个“模板 + 月份”最多生成一次流水。应用层先检查生成记录，数据库唯一约束
 * 再处理并发请求；家长处理全家模板，普通成员只能处理自己的模板。</p>
 */
@Service
public class RecurringService {
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM")
            .withResolverStyle(ResolverStyle.STRICT);

    private final RecurringTemplateMapper templateMapper;
    private final RecurringGenerationMapper generationMapper;
    private final LedgerEntryMapper entryMapper;
    private final AppUserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final CategoryService categoryService;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public RecurringService(RecurringTemplateMapper templateMapper, RecurringGenerationMapper generationMapper,
                            LedgerEntryMapper entryMapper, AppUserMapper userMapper, CategoryMapper categoryMapper,
                            CategoryService categoryService, CurrentUserService currentUserService,
                            AuditLogService auditLogService) {
        this.templateMapper = templateMapper;
        this.generationMapper = generationMapper;
        this.entryMapper = entryMapper;
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
        this.categoryService = categoryService;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    public List<RecurringTemplateResponse> list() {
        CurrentUser user = currentUserService.requireHouseholdUser();
        QueryWrapper<RecurringTemplate> wrapper = new QueryWrapper<>();
        wrapper.eq("household_id", user.householdId());
        if (!user.isParent()) wrapper.eq("member_id", user.id());
        wrapper.orderByDesc("created_at");
        List<RecurringTemplate> templates = templateMapper.selectList(wrapper);
        if (templates.isEmpty()) return List.of();
        Map<Long, AppUser> members = memberMap(templates);
        Map<Long, FinanceCategory> categories = categoryMap(templates);
        return templates.stream().map(template -> response(template, members.get(template.getMemberId()),
                categories.get(template.getCategoryId()))).toList();
    }

    @Transactional
    public RecurringTemplateResponse create(RecurringTemplateRequest request) {
        CurrentUser user = currentUserService.requireHouseholdUser();
        AppUser member = resolveMember(request.memberId(), user);
        FinanceCategory category = resolveCategory(request.categoryId(), request.type(), user.householdId(), false);
        validateTemplate(request.amount(), request.dayOfMonth());
        RecurringTemplate template = new RecurringTemplate();
        template.setHouseholdId(user.householdId());
        template.setMemberId(member.getId());
        template.setCategoryId(category.getId());
        template.setType(request.type());
        template.setAmount(request.amount());
        template.setDayOfMonth(request.dayOfMonth());
        template.setNote(cleanNote(request.note()));
        template.setStatus(RecurringStatus.ACTIVE);
        templateMapper.insert(template);
        recordAudit(user, "RECURRING_CREATE", template.getId(), "新增周期流水模板");
        return response(template, member, category);
    }

    @Transactional
    public RecurringTemplateResponse update(Long id, RecurringTemplateRequest request) {
        CurrentUser user = currentUserService.requireHouseholdUser();
        RecurringTemplate template = target(id, user);
        AppUser member = resolveMember(request.memberId(), user);
        FinanceCategory category = resolveCategory(request.categoryId(), request.type(), user.householdId(),
                template.getCategoryId().equals(request.categoryId()));
        validateTemplate(request.amount(), request.dayOfMonth());
        template.setMemberId(member.getId());
        template.setCategoryId(category.getId());
        template.setType(request.type());
        template.setAmount(request.amount());
        template.setDayOfMonth(request.dayOfMonth());
        template.setNote(cleanNote(request.note()));
        templateMapper.updateById(template);
        recordAudit(user, "RECURRING_UPDATE", template.getId(), "修改周期流水模板");
        return response(template, member, category);
    }

    @Transactional
    public RecurringTemplateResponse updateStatus(Long id, boolean active) {
        CurrentUser user = currentUserService.requireHouseholdUser();
        RecurringTemplate template = target(id, user);
        template.setStatus(active ? RecurringStatus.ACTIVE : RecurringStatus.INACTIVE);
        templateMapper.updateById(template);
        recordAudit(user, "RECURRING_STATUS", template.getId(), active ? "启用周期流水模板" : "停用周期流水模板");
        return response(template, userMapper.selectById(template.getMemberId()),
                categoryMapper.selectById(template.getCategoryId()));
    }

    @Transactional
    public void delete(Long id) {
        CurrentUser user = currentUserService.requireHouseholdUser();
        RecurringTemplate template = target(id, user);
        templateMapper.deleteById(template.getId());
        recordAudit(user, "RECURRING_DELETE", template.getId(), "删除周期流水模板");
    }

    @Transactional
    public RecurringGenerationResponse generate(YearMonth month) {
        if (month == null) throw badMonth();
        CurrentUser user = currentUserService.requireHouseholdUser();
        QueryWrapper<RecurringTemplate> wrapper = new QueryWrapper<>();
        wrapper.eq("household_id", user.householdId());
        if (!user.isParent()) wrapper.eq("member_id", user.id());
        wrapper.orderByAsc("id");
        List<RecurringTemplate> templates = templateMapper.selectList(wrapper);
        LocalDate generatedMonth = month.atDay(1);
        int generated = 0;
        int alreadyGenerated = 0;
        List<RecurringGenerationResponse.SkippedTemplate> skips = new java.util.ArrayList<>();
        for (RecurringTemplate template : templates) {
            // 快速处理正常的重复点击；数据库唯一约束仍是并发下的最终保障。
            RecurringGeneration existing = generationMapper.selectOne(new QueryWrapper<RecurringGeneration>()
                    .eq("recurring_template_id", template.getId())
                    .eq("generated_month", generatedMonth));
            if (existing != null) {
                alreadyGenerated++;
                continue;
            }
            String skipReason = skipReason(template, month);
            if (skipReason != null) {
                skips.add(new RecurringGenerationResponse.SkippedTemplate(template.getId(), skipReason));
                continue;
            }
            AppUser member = userMapper.selectById(template.getMemberId());
            FinanceCategory category;
            try {
                category = categoryService.requireUsable(template.getCategoryId(), template.getHouseholdId(),
                        CategoryType.valueOf(template.getType().name()), false);
            } catch (ApiException exception) {
                skips.add(new RecurringGenerationResponse.SkippedTemplate(template.getId(), "分类不可用：" + exception.getMessage()));
                continue;
            }
            // 29—31 日在短月份落到当月最后一天，保证生成结果仍属于目标月份。
            LocalDate occurredOn = month.atDay(Math.min(template.getDayOfMonth(), month.lengthOfMonth()));
            LedgerEntry entry = new LedgerEntry();
            entry.setHouseholdId(template.getHouseholdId());
            entry.setMemberId(member.getId());
            entry.setCategoryId(category.getId());
            entry.setType(template.getType());
            entry.setAmount(template.getAmount());
            entry.setOccurredOn(occurredOn);
            entry.setNote(template.getNote());
            entry.setDeleted(false);
            entryMapper.insert(entry);

            RecurringGeneration generation = new RecurringGeneration();
            generation.setRecurringTemplateId(template.getId());
            generation.setGeneratedMonth(generatedMonth);
            generation.setLedgerEntryId(entry.getId());
            try {
                generationMapper.insert(generation);
                generated++;
            } catch (DataIntegrityViolationException duplicate) {
                // 并发请求已占用“模板 + 月份”时，撤销本请求刚插入的孤立流水。
                if (entry.getId() != null) entryMapper.deleteById(entry.getId());
                alreadyGenerated++;
            }
        }
        if (generated > 0) {
            recordAudit(user, "RECURRING_GENERATE", null,
                    "生成" + month + "周期流水 " + generated + "笔");
        }
        return new RecurringGenerationResponse(month.toString(), generated, alreadyGenerated, skips.size(), skips);
    }

    public RecurringGenerationResponse generate(String month) {
        try {
            return generate(YearMonth.parse(month, MONTH_FORMAT));
        } catch (DateTimeParseException | NullPointerException exception) {
            throw badMonth();
        }
    }

    private String skipReason(RecurringTemplate template, YearMonth month) {
        if (template.getStatus() != RecurringStatus.ACTIVE) return "模板已停用";
        AppUser member = userMapper.selectById(template.getMemberId());
        if (member == null || !template.getHouseholdId().equals(member.getHouseholdId())) return "成员不存在或不属于当前家庭";
        if (member.getStatus() != UserStatus.ACTIVE) return "成员已停用";
        if (template.getType() == null || template.getDayOfMonth() == null) return "模板参数无效";
        LocalDate dueDate = month.atDay(Math.min(template.getDayOfMonth(), month.lengthOfMonth()));
        if (dueDate.isAfter(LocalDate.now())) return "本月尚未到期";
        return null;
    }

    private AppUser resolveMember(Long requestedId, CurrentUser user) {
        // 普通成员不能借请求参数替其他成员创建模板。
        Long memberId = user.isParent() ? requestedId : user.id();
        if (memberId == null) throw new ApiException(HttpStatus.BAD_REQUEST, "MEMBER_REQUIRED", "请选择归属成员");
        AppUser member = userMapper.selectById(memberId);
        if (member == null || !user.householdId().equals(member.getHouseholdId())
                || member.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MEMBER_INVALID", "归属成员不存在或已停用");
        }
        return member;
    }

    private FinanceCategory resolveCategory(Long id, com.family.finance.ledger.domain.LedgerType type, Long householdId,
                                            boolean allowInactive) {
        if (type == null) throw new ApiException(HttpStatus.BAD_REQUEST, "TYPE_REQUIRED", "收支类型不能为空");
        return categoryService.requireUsable(id, householdId, CategoryType.valueOf(type.name()), allowInactive);
    }

    private RecurringTemplate target(Long id, CurrentUser user) {
        RecurringTemplate template = templateMapper.selectById(id);
        // 不区分“不存在”和“无权访问”，避免暴露其他家庭的模板标识。
        if (template == null || !user.householdId().equals(template.getHouseholdId())
                || (!user.isParent() && !user.id().equals(template.getMemberId()))) {
            throw new ApiException(HttpStatus.NOT_FOUND, "RECURRING_TEMPLATE_NOT_FOUND", "周期模板不存在或无权访问");
        }
        return template;
    }

    private void validateTemplate(BigDecimal amount, Integer dayOfMonth) {
        if (amount == null || amount.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AMOUNT_INVALID", "金额必须大于0");
        }
        if (dayOfMonth == null || dayOfMonth < 1 || dayOfMonth > 31) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DAY_INVALID", "发生日必须在1到31之间");
        }
    }

    private RecurringTemplateResponse response(RecurringTemplate template, AppUser member, FinanceCategory category) {
        return new RecurringTemplateResponse(template.getId(), template.getMemberId(), member == null ? null : member.getDisplayName(),
                member == null ? null : member.getMemberNo(), template.getType(), template.getCategoryId(),
                category == null ? null : category.getName(), template.getAmount(), template.getDayOfMonth(),
                template.getNote(), template.getStatus(), template.getCreatedAt(), template.getUpdatedAt());
    }

    private Map<Long, AppUser> memberMap(List<RecurringTemplate> templates) {
        if (templates.isEmpty()) return Collections.emptyMap();
        return userMapper.selectBatchIds(templates.stream().map(RecurringTemplate::getMemberId).distinct().toList())
                .stream().collect(Collectors.toMap(AppUser::getId, Function.identity()));
    }

    private Map<Long, FinanceCategory> categoryMap(List<RecurringTemplate> templates) {
        if (templates.isEmpty()) return Collections.emptyMap();
        return categoryMapper.selectBatchIds(templates.stream().map(RecurringTemplate::getCategoryId).distinct().toList())
                .stream().collect(Collectors.toMap(FinanceCategory::getId, Function.identity()));
    }

    private String cleanNote(String note) { return note == null ? null : note.trim(); }

    private void recordAudit(CurrentUser actor, String action, Long objectId, String summary) {
        if (auditLogService != null) auditLogService.record(actor, action, "RECURRING_TEMPLATE", objectId, summary);
    }

    private ApiException badMonth() {
        return new ApiException(HttpStatus.BAD_REQUEST, "MONTH_INVALID", "月份必须使用YYYY-MM格式");
    }
}
