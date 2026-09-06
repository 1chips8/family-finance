package com.family.finance.ledger;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.family.finance.audit.service.AuditLogService;
import com.family.finance.auth.domain.AppUser;
import com.family.finance.auth.domain.Role;
import com.family.finance.auth.domain.UserStatus;
import com.family.finance.auth.mapper.AppUserMapper;
import com.family.finance.category.domain.CategoryScope;
import com.family.finance.category.domain.CategoryStatus;
import com.family.finance.category.domain.CategoryType;
import com.family.finance.category.domain.FinanceCategory;
import com.family.finance.category.mapper.CategoryMapper;
import com.family.finance.common.error.ApiException;
import com.family.finance.common.security.CurrentUser;
import com.family.finance.common.security.CurrentUserService;
import com.family.finance.ledger.csv.CsvCodec;
import com.family.finance.ledger.domain.LedgerType;
import com.family.finance.ledger.dto.EntryImportRequest;
import com.family.finance.ledger.service.EntryCsvService;
import com.family.finance.ledger.service.LedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntryCsvServiceTest {
    @Mock CurrentUserService currentUserService;
    @Mock AppUserMapper userMapper;
    @Mock CategoryMapper categoryMapper;
    @Mock LedgerService ledgerService;
    @Mock AuditLogService auditLogService;
    private EntryCsvService service;
    private CurrentUser parent;

    @BeforeEach
    void setUp() {
        service = new EntryCsvService(currentUserService, userMapper, categoryMapper, ledgerService, auditLogService);
        parent = new CurrentUser(1L, "parent", "家长", 9L, "M001", Role.PARENT, UserStatus.ACTIVE);
        lenient().when(currentUserService.requireHouseholdUser()).thenReturn(parent);
        AppUser member = new AppUser(); member.setId(1L); member.setHouseholdId(9L); member.setMemberNo("M001");
        member.setDisplayName("家长"); member.setStatus(UserStatus.ACTIVE);
        FinanceCategory category = new FinanceCategory(); category.setId(7L); category.setHouseholdId(9L);
        category.setName("餐饮"); category.setType(CategoryType.EXPENSE); category.setScope(CategoryScope.CUSTOM);
        category.setStatus(CategoryStatus.ACTIVE);
        lenient().when(userMapper.selectList(any(Wrapper.class))).thenReturn(List.of(member));
        lenient().when(categoryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(category));
    }

    @Test
    void previewsQuotedCsvAndCommitsTheSameContent() {
        String csv = "发生日期,类型,分类,成员编号,金额,备注\r\n"
                + "2026-09-01,支出,餐饮,M001,12.30,\"午饭,两人\"\r\n";

        var preview = service.preview(csv);
        var committed = service.commit(new EntryImportRequest(csv, preview.checksum()));

        assertThat(preview.validRows()).isEqualTo(1);
        assertThat(preview.rows().get(0).note()).isEqualTo("午饭,两人");
        assertThat(preview.rows().get(0).amount()).isEqualByComparingTo(new BigDecimal("12.30"));
        assertThat(committed.importedRows()).isEqualTo(1);
        verify(ledgerService).createImported(eq(parent), any());
        verify(auditLogService).record(parent, "ENTRY_IMPORT", "ENTRY", null, "批量导入 1 笔流水");
    }

    @Test
    void rejectsAnUnexpectedHeader() {
        assertThatThrownBy(() -> service.preview("日期,金额\r\n2026-09-01,1\r\n"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("列名必须");
    }

    @Test
    void rejectsAmbiguousSystemAndHouseholdCategoryNames() {
        FinanceCategory system = new FinanceCategory(); system.setId(7L); system.setName("餐饮");
        system.setType(CategoryType.EXPENSE); system.setScope(CategoryScope.SYSTEM); system.setStatus(CategoryStatus.ACTIVE);
        FinanceCategory custom = new FinanceCategory(); custom.setId(8L); custom.setHouseholdId(9L); custom.setName("餐饮");
        custom.setType(CategoryType.EXPENSE); custom.setScope(CategoryScope.CUSTOM); custom.setStatus(CategoryStatus.ACTIVE);
        when(categoryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(system, custom));
        String csv = "发生日期,类型,分类,成员编号,金额,备注\r\n2026-09-01,支出,餐饮,M001,12.30,午饭\r\n";

        var preview = service.preview(csv);

        assertThat(preview.validRows()).isZero();
        assertThat(preview.rows().get(0).errors().get("category")).contains("同名分类");
    }

    @Test
    void codecRoundTripsQuotesAndNewlines() {
        String encoded = CsvCodec.line(List.of("餐饮", "午饭,两人", "他说\"好\""));
        assertThat(CsvCodec.parse(encoded).get(0)).containsExactly("餐饮", "午饭,两人", "他说\"好\"");
        assertThat(CsvCodec.spreadsheetSafe("=HYPERLINK(\"bad\")")).startsWith("'=");
    }
}
