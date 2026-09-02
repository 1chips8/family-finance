package com.family.finance.ledger;

import com.family.finance.auth.domain.AppUser;
import com.family.finance.auth.domain.Role;
import com.family.finance.auth.domain.UserStatus;
import com.family.finance.auth.mapper.AppUserMapper;
import com.family.finance.category.domain.CategoryStatus;
import com.family.finance.category.domain.CategoryType;
import com.family.finance.category.domain.FinanceCategory;
import com.family.finance.category.mapper.CategoryMapper;
import com.family.finance.category.service.CategoryService;
import com.family.finance.common.security.CurrentUser;
import com.family.finance.common.security.CurrentUserService;
import com.family.finance.ledger.domain.LedgerEntry;
import com.family.finance.ledger.dto.CreateEntryRequest;
import com.family.finance.ledger.mapper.LedgerEntryMapper;
import com.family.finance.ledger.service.LedgerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {
    @Mock LedgerEntryMapper entryMapper;
    @Mock AppUserMapper userMapper;
    @Mock CategoryMapper categoryMapper;
    @Mock CategoryService categoryService;
    @Mock CurrentUserService currentUserService;
    @InjectMocks LedgerService ledgerService;

    @Test
    void ordinaryMemberCannotChooseAnotherMemberForANewEntry() {
        CurrentUser current = new CurrentUser(2L, "member", "成员", 9L, "M002", Role.MEMBER, UserStatus.ACTIVE);
        AppUser member = new AppUser(); member.setId(2L); member.setHouseholdId(9L); member.setStatus(UserStatus.ACTIVE); member.setDisplayName("成员"); member.setMemberNo("M002");
        FinanceCategory category = new FinanceCategory(); category.setId(7L); category.setType(CategoryType.EXPENSE); category.setStatus(CategoryStatus.ACTIVE); category.setName("餐饮");
        when(currentUserService.requireHouseholdUser()).thenReturn(current);
        when(userMapper.selectById(2L)).thenReturn(member);
        when(categoryService.requireUsable(7L, 9L, CategoryType.EXPENSE, false)).thenReturn(category);

        ledgerService.create(new CreateEntryRequest(99L, com.family.finance.ledger.domain.LedgerType.EXPENSE, 7L,
                new BigDecimal("12.30"), LocalDate.now(), " 午饭 "));

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(entryMapper).insert(captor.capture());
        assertThat(captor.getValue().getMemberId()).isEqualTo(2L);
        assertThat(captor.getValue().getNote()).isEqualTo("午饭");
    }
}
