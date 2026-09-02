package com.family.finance.common.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.family.finance.auth.domain.AppUser;
import com.family.finance.auth.domain.Role;
import com.family.finance.auth.domain.UserStatus;
import com.family.finance.auth.mapper.AppUserMapper;
import com.family.finance.category.domain.*;
import com.family.finance.category.mapper.CategoryMapper;
import com.family.finance.household.domain.Household;
import com.family.finance.household.mapper.HouseholdMapper;
import com.family.finance.ledger.domain.LedgerEntry;
import com.family.finance.ledger.domain.LedgerType;
import com.family.finance.ledger.mapper.LedgerEntryMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration
public class DemoDataInitializer {
    @Bean
    @ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
    DemoRunner demoRunner(AppUserMapper userMapper, HouseholdMapper householdMapper, CategoryMapper categoryMapper,
                          LedgerEntryMapper entryMapper, PasswordEncoder passwordEncoder,
                          @Value("${app.demo.password:Demo1234}") String password) {
        return new DemoRunner(userMapper, householdMapper, categoryMapper, entryMapper, passwordEncoder, password);
    }

    static class DemoRunner implements org.springframework.boot.ApplicationRunner {
        private final AppUserMapper userMapper;
        private final HouseholdMapper householdMapper;
        private final CategoryMapper categoryMapper;
        private final LedgerEntryMapper entryMapper;
        private final PasswordEncoder passwordEncoder;
        private final String password;

        DemoRunner(AppUserMapper userMapper, HouseholdMapper householdMapper, CategoryMapper categoryMapper,
                   LedgerEntryMapper entryMapper, PasswordEncoder passwordEncoder, String password) {
            this.userMapper = userMapper; this.householdMapper = householdMapper; this.categoryMapper = categoryMapper;
            this.entryMapper = entryMapper; this.passwordEncoder = passwordEncoder; this.password = password;
        }

        @Override
        @Transactional
        public void run(org.springframework.boot.ApplicationArguments args) {
            if (userMapper.selectByUsername("demo_parent") != null) return;
            AppUser parent = newUser("demo_parent", "演示家长");
            AppUser member = newUser("demo_member", "演示成员");
            AppUser guest = newUser("demo_guest", "待入户用户");
            userMapper.insert(parent); userMapper.insert(member); userMapper.insert(guest);

            Household household = new Household();
            household.setName("家账演示家庭"); household.setInviteCode("DEMO2026"); household.setCreatedBy(parent.getId());
            householdMapper.insert(household);
            parent.setHouseholdId(household.getId()); parent.setMemberNo("M001"); parent.setRole(Role.PARENT);
            member.setHouseholdId(household.getId()); member.setMemberNo("M002"); member.setRole(Role.MEMBER);
            userMapper.updateById(parent); userMapper.updateById(member);

            FinanceCategory education = new FinanceCategory();
            education.setHouseholdId(household.getId()); education.setScope(CategoryScope.CUSTOM);
            education.setType(CategoryType.EXPENSE); education.setName("教育"); education.setStatus(CategoryStatus.ACTIVE);
            categoryMapper.insert(education);
            FinanceCategory salary = category("工资", CategoryType.INCOME);
            FinanceCategory food = category("餐饮", CategoryType.EXPENSE);
            for (int i = 0; i < 12; i++) {
                LocalDate date = LocalDate.now().withDayOfMonth(1).minusMonths(11 - i).plusDays(2);
                if (!date.plusDays(8).isBefore(LocalDate.now())) date = LocalDate.now().minusDays(9);
                addEntry(household, parent, salary, LedgerType.INCOME, new BigDecimal("12000.00"), date, "月度工资");
                addEntry(household, member, food, LedgerType.EXPENSE, new BigDecimal("680.50"), date.plusDays(5), "家庭餐饮");
                addEntry(household, parent, education, LedgerType.EXPENSE, new BigDecimal("320.00"), date.plusDays(8), "学习用品");
            }
        }

        private AppUser newUser(String username, String displayName) {
            AppUser user = new AppUser(); user.setUsername(username); user.setDisplayName(displayName);
            user.setPasswordHash(passwordEncoder.encode(password)); user.setStatus(UserStatus.ACTIVE); return user;
        }
        private FinanceCategory category(String name, CategoryType type) {
            return categoryMapper.selectOne(new QueryWrapper<FinanceCategory>().isNull("household_id").eq("name", name).eq("type", type));
        }
        private void addEntry(Household household, AppUser member, FinanceCategory category, LedgerType type,
                              BigDecimal amount, LocalDate date, String note) {
            LedgerEntry entry = new LedgerEntry(); entry.setHouseholdId(household.getId()); entry.setMemberId(member.getId());
            entry.setCategoryId(category.getId()); entry.setType(type); entry.setAmount(amount); entry.setOccurredOn(date);
            entry.setNote(note); entry.setDeleted(false); entryMapper.insert(entry);
        }
    }
}
