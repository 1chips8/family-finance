package com.family.finance.recurring;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.family.finance.audit.service.AuditLogService;
import com.family.finance.auth.domain.AppUser;
import com.family.finance.auth.domain.Role;
import com.family.finance.auth.domain.UserStatus;
import com.family.finance.auth.mapper.AppUserMapper;
import com.family.finance.category.domain.CategoryStatus;
import com.family.finance.category.domain.CategoryType;
import com.family.finance.category.domain.FinanceCategory;
import com.family.finance.category.mapper.CategoryMapper;
import com.family.finance.category.service.CategoryService;
import com.family.finance.common.error.ApiException;
import com.family.finance.common.security.CurrentUser;
import com.family.finance.common.security.CurrentUserService;
import com.family.finance.ledger.domain.LedgerEntry;
import com.family.finance.ledger.domain.LedgerType;
import com.family.finance.ledger.mapper.LedgerEntryMapper;
import com.family.finance.recurring.domain.RecurringGeneration;
import com.family.finance.recurring.domain.RecurringStatus;
import com.family.finance.recurring.domain.RecurringTemplate;
import com.family.finance.recurring.dto.RecurringGenerationResponse;
import com.family.finance.recurring.dto.RecurringTemplateRequest;
import com.family.finance.recurring.mapper.RecurringGenerationMapper;
import com.family.finance.recurring.mapper.RecurringTemplateMapper;
import com.family.finance.recurring.service.RecurringService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringServiceTest {
    @Mock RecurringTemplateMapper templateMapper;
    @Mock RecurringGenerationMapper generationMapper;
    @Mock LedgerEntryMapper entryMapper;
    @Mock AppUserMapper userMapper;
    @Mock CategoryMapper categoryMapper;
    @Mock CategoryService categoryService;
    @Mock CurrentUserService currentUserService;
    @Mock AuditLogService auditLogService;
    @InjectMocks RecurringService recurringService;

    @Test
    void ordinaryMemberOwnsTemplateEvenWhenRequestNamesAnotherMember() {
        CurrentUser current = member(2L, 9L);
        when(currentUserService.requireHouseholdUser()).thenReturn(current);
        when(userMapper.selectById(2L)).thenReturn(activeMember(2L, 9L));
        FinanceCategory category = category(7L);
        when(categoryService.requireUsable(7L, 9L, CategoryType.EXPENSE, false)).thenReturn(category);

        recurringService.create(request(99L, 7L));

        ArgumentCaptor<RecurringTemplate> captor = ArgumentCaptor.forClass(RecurringTemplate.class);
        verify(templateMapper).insert(captor.capture());
        assertThat(captor.getValue().getMemberId()).isEqualTo(2L);
        assertThat(captor.getValue().getNote()).isEqualTo("房租");
    }

    @Test
    void rejectsTemplateFromAnotherHousehold() {
        CurrentUser current = new CurrentUser(1L, "parent", "家长", 9L, "M001", Role.PARENT, UserStatus.ACTIVE);
        RecurringTemplate template = template(4L, 88L, 2L, 7L);
        when(currentUserService.requireHouseholdUser()).thenReturn(current);
        when(templateMapper.selectById(4L)).thenReturn(template);

        assertThatThrownBy(() -> recurringService.updateStatus(4L, false))
                .isInstanceOf(ApiException.class).hasMessage("周期模板不存在或无权访问");
        verify(templateMapper, never()).updateById(any(RecurringTemplate.class));
    }

    @Test
    void clampsMonthEndAndSecondGenerationIsIdempotent() {
        CurrentUser current = new CurrentUser(1L, "parent", "家长", 9L, "M001", Role.PARENT, UserStatus.ACTIVE);
        RecurringTemplate template = template(4L, 9L, 2L, 7L);
        template.setDayOfMonth(31);
        when(currentUserService.requireHouseholdUser()).thenReturn(current);
        when(templateMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(template));
        when(generationMapper.selectOne(any(QueryWrapper.class))).thenReturn(null, generation(4L, LocalDate.of(2024, 2, 1)));
        when(userMapper.selectById(2L)).thenReturn(activeMember(2L, 9L));
        when(categoryService.requireUsable(7L, 9L, CategoryType.EXPENSE, false)).thenReturn(category(7L));
        doAnswer(invocation -> { invocation.<LedgerEntry>getArgument(0).setId(101L); return 1; })
                .when(entryMapper).insert(any(LedgerEntry.class));

        RecurringGenerationResponse first = recurringService.generate(YearMonth.of(2024, 2));
        RecurringGenerationResponse second = recurringService.generate(YearMonth.of(2024, 2));

        assertThat(first.generated()).isEqualTo(1);
        assertThat(second.alreadyGenerated()).isEqualTo(1);
        ArgumentCaptor<LedgerEntry> entry = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(entryMapper).insert(entry.capture());
        assertThat(entry.getValue().getOccurredOn()).isEqualTo(LocalDate.of(2024, 2, 29));
    }

    @Test
    void skipsInactiveMemberAndCategoryWithoutCreatingLedgerEntry() {
        CurrentUser current = new CurrentUser(1L, "parent", "家长", 9L, "M001", Role.PARENT, UserStatus.ACTIVE);
        RecurringTemplate memberTemplate = template(4L, 9L, 2L, 7L);
        RecurringTemplate categoryTemplate = template(5L, 9L, 3L, 8L);
        when(currentUserService.requireHouseholdUser()).thenReturn(current);
        when(templateMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(memberTemplate, categoryTemplate));
        when(generationMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        when(userMapper.selectById(2L)).thenReturn(inactiveMember(2L, 9L));
        when(userMapper.selectById(3L)).thenReturn(activeMember(3L, 9L));
        when(categoryService.requireUsable(8L, 9L, CategoryType.EXPENSE, false))
                .thenThrow(new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "CATEGORY_INVALID", "分类已停用"));

        RecurringGenerationResponse result = recurringService.generate(YearMonth.of(2024, 1));

        assertThat(result.generated()).isZero();
        assertThat(result.skipped()).isEqualTo(2);
        assertThat(result.skipReasons()).extracting(RecurringGenerationResponse.SkippedTemplate::reason)
                .containsExactly("成员已停用", "分类不可用：分类已停用");
        verifyNoInteractions(entryMapper);
    }

    private RecurringTemplateRequest request(Long memberId, Long categoryId) {
        return new RecurringTemplateRequest(memberId, LedgerType.EXPENSE, categoryId,
                new BigDecimal("12.30"), 15, " 房租 ");
    }

    private RecurringTemplate template(Long id, Long householdId, Long memberId, Long categoryId) {
        RecurringTemplate template = new RecurringTemplate();
        template.setId(id); template.setHouseholdId(householdId); template.setMemberId(memberId);
        template.setCategoryId(categoryId); template.setType(LedgerType.EXPENSE);
        template.setAmount(new BigDecimal("100.00")); template.setDayOfMonth(10);
        template.setStatus(RecurringStatus.ACTIVE); return template;
    }

    private RecurringGeneration generation(Long templateId, LocalDate month) {
        RecurringGeneration generation = new RecurringGeneration();
        generation.setRecurringTemplateId(templateId); generation.setGeneratedMonth(month); return generation;
    }

    private FinanceCategory category(Long id) {
        FinanceCategory category = new FinanceCategory(); category.setId(id); category.setType(CategoryType.EXPENSE);
        category.setStatus(CategoryStatus.ACTIVE); category.setName("住房"); return category;
    }

    private AppUser activeMember(Long id, Long householdId) {
        AppUser user = new AppUser(); user.setId(id); user.setHouseholdId(householdId);
        user.setStatus(UserStatus.ACTIVE); user.setMemberNo("M00" + id); user.setDisplayName("成员"); return user;
    }

    private AppUser inactiveMember(Long id, Long householdId) {
        AppUser user = activeMember(id, householdId); user.setStatus(UserStatus.INACTIVE); return user;
    }

    private CurrentUser member(Long id, Long householdId) {
        return new CurrentUser(id, "member", "成员", householdId, "M002", Role.MEMBER, UserStatus.ACTIVE);
    }
}
