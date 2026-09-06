# Family Monthly Report Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Follow this plan task-by-task with test-first changes. The project collaboration rules permit only the brainstorming and writing-plans skills, so execution uses the repository's normal Maven, Vitest, and browser-validation workflow.

**Goal:** Turn the existing family ledger into a memorable style-C “家庭月报” product with budgets, recurring entries, CSV import/export, enhanced statistics, audit logs, and the approved frontend fixes.

**Architecture:** Add one Flyway V2 migration and four focused backend modules that follow the existing Controller → Service → Mapper pattern. Extend the existing dashboard response rather than creating a parallel reporting backend. On the frontend, introduce route-level lazy loading, feature APIs/types/views, and a CSS-based editorial visual system without adding runtime dependencies.

**Tech Stack:** Java 17, Spring Boot 3.5, Spring Security, MyBatis-Plus, Flyway, MySQL 8.4, Vue 3.5, TypeScript, Pinia, Vue Router, Element Plus, ECharts, JUnit, Vitest.

---

### Task 1: Add the V2 schema

**Files:**
- Create: `backend/src/main/resources/db/migration/V2__add_monthly_finance_features.sql`
- Modify: `backend/src/test/java/com/family/finance/DatabaseMigrationTest.java`

- [ ] **Step 1: Extend the migration test to assert the new tables and uniqueness constraints**

Add assertions that `monthly_budget`, `recurring_template`, `recurring_generation`, and `audit_log` exist after Flyway migration, and that duplicate `(recurring_template_id, generated_month)` inserts fail.

- [ ] **Step 2: Run the migration test and verify it fails**

Run: `backend\mvnw.cmd -f backend\pom.xml -Dtest=DatabaseMigrationTest test`

Expected: FAIL because V2 tables do not exist.

- [ ] **Step 3: Create the V2 migration**

Create tables with these stable keys:

```sql
CREATE TABLE monthly_budget (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  household_id BIGINT NOT NULL,
  budget_month DATE NOT NULL,
  category_id BIGINT NULL,
  amount DECIMAL(12,2) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_budget_household FOREIGN KEY (household_id) REFERENCES household(id) ON DELETE CASCADE,
  CONSTRAINT fk_budget_category FOREIGN KEY (category_id) REFERENCES finance_category(id) ON DELETE CASCADE,
  CONSTRAINT chk_budget_amount CHECK (amount > 0),
  UNIQUE KEY uk_budget_household_month_category (household_id, budget_month, category_id)
);
```

Use an additional generated discriminator column so MySQL also enforces one total budget where `category_id IS NULL`. Define the recurring template, recurring generation, and audit log tables with household indexes, foreign keys, status/type checks, and the unique generation key.

- [ ] **Step 4: Run the migration test**

Expected: PASS.

- [ ] **Step 5: Commit**

`git commit -m "feat: add monthly finance schema"`

### Task 2: Add shared audit logging

**Files:**
- Create: `backend/src/main/java/com/family/finance/audit/domain/AuditLog.java`
- Create: `backend/src/main/java/com/family/finance/audit/mapper/AuditLogMapper.java`
- Create: `backend/src/main/java/com/family/finance/audit/dto/AuditLogResponse.java`
- Create: `backend/src/main/java/com/family/finance/audit/service/AuditLogService.java`
- Create: `backend/src/main/java/com/family/finance/audit/web/AuditLogController.java`
- Create: `backend/src/test/java/com/family/finance/audit/AuditLogServiceTest.java`
- Modify: existing member, category, ledger, household, budget, recurring, and import services as they are implemented

- [ ] **Step 1: Write tests for household filtering, parent-only listing, and safe summaries**

Test that a parent receives only its household logs, a member receives 403, and summaries never include request payloads or credentials.

- [ ] **Step 2: Run the audit test and verify it fails**

Run: `backend\mvnw.cmd -f backend\pom.xml -Dtest=AuditLogServiceTest test`

- [ ] **Step 3: Implement the audit service contract**

```java
public void record(CurrentUser actor, String action, String objectType, Long objectId, String summary);
public PageResponse<AuditLogResponse> list(int page, int pageSize);
```

`record` accepts an already-authenticated actor and a server-authored summary. `list` calls `requireParent()` and filters by `household_id`.

- [ ] **Step 4: Add audit calls inside the same transactions as business writes**

Record successful writes only. Use concise summaries such as `新增支出流水 ¥88.88` and never serialize DTOs.

- [ ] **Step 5: Run the audit test and affected service tests**

Expected: PASS.

- [ ] **Step 6: Commit**

`git commit -m "feat: add household audit log"`

### Task 3: Implement monthly budgets

**Files:**
- Create: `backend/src/main/java/com/family/finance/budget/domain/MonthlyBudget.java`
- Create: `backend/src/main/java/com/family/finance/budget/mapper/MonthlyBudgetMapper.java`
- Create: `backend/src/main/java/com/family/finance/budget/dto/BudgetRequest.java`
- Create: `backend/src/main/java/com/family/finance/budget/dto/BudgetOverviewResponse.java`
- Create: `backend/src/main/java/com/family/finance/budget/service/BudgetService.java`
- Create: `backend/src/main/java/com/family/finance/budget/web/BudgetController.java`
- Create: `backend/src/test/java/com/family/finance/budget/BudgetServiceTest.java`

- [ ] **Step 1: Write failing budget tests**

Cover parent upsert/delete, member read-only behavior, cross-household category rejection, total/category uniqueness, and spent/remaining/status calculation.

- [ ] **Step 2: Run tests and verify failure**

Run: `backend\mvnw.cmd -f backend\pom.xml -Dtest=BudgetServiceTest test`

- [ ] **Step 3: Implement the service**

```java
public BudgetOverviewResponse overview(YearMonth month);
public BudgetOverviewResponse upsertTotal(YearMonth month, BigDecimal amount);
public BudgetOverviewResponse upsertCategory(Long categoryId, YearMonth month, BigDecimal amount);
public void deleteTotal(YearMonth month);
public void deleteCategory(Long categoryId, YearMonth month);
```

Validate positive amounts and expense-category ownership. Calculate actual spending from non-deleted ledger rows for the selected month.

- [ ] **Step 4: Add REST endpoints and stable error responses**

Use `@DateTimeFormat(pattern = "yyyy-MM")` only where conversion is reliable; otherwise accept a string and return `INVALID_MONTH` with HTTP 400.

- [ ] **Step 5: Run budget and existing ledger tests**

Expected: PASS.

- [ ] **Step 6: Commit**

`git commit -m "feat: add household monthly budgets"`

### Task 4: Implement recurring-entry templates and idempotent generation

**Files:**
- Create: `backend/src/main/java/com/family/finance/recurring/domain/RecurringTemplate.java`
- Create: `backend/src/main/java/com/family/finance/recurring/domain/RecurringGeneration.java`
- Create: `backend/src/main/java/com/family/finance/recurring/mapper/RecurringTemplateMapper.java`
- Create: `backend/src/main/java/com/family/finance/recurring/mapper/RecurringGenerationMapper.java`
- Create: `backend/src/main/java/com/family/finance/recurring/dto/RecurringTemplateRequest.java`
- Create: `backend/src/main/java/com/family/finance/recurring/dto/RecurringTemplateResponse.java`
- Create: `backend/src/main/java/com/family/finance/recurring/dto/RecurringGenerationResponse.java`
- Create: `backend/src/main/java/com/family/finance/recurring/service/RecurringService.java`
- Create: `backend/src/main/java/com/family/finance/recurring/web/RecurringController.java`
- Create: `backend/src/test/java/com/family/finance/recurring/RecurringServiceTest.java`
- Modify: `backend/src/main/java/com/family/finance/ledger/service/LedgerService.java`

- [ ] **Step 1: Write failing ownership and idempotency tests**

Cover member-own templates, parent templates for active household members, cross-household rejection, month-end clamping, inactive category/member skips, and duplicate generation.

- [ ] **Step 2: Run the recurring tests and verify failure**

- [ ] **Step 3: Add a trusted ledger creation method**

Refactor the existing validation and insert logic into one package-visible or public service method used by both REST creation and recurring generation. Do not duplicate ledger rules.

- [ ] **Step 4: Implement template CRUD and generation**

```java
public List<RecurringTemplateResponse> list();
public RecurringTemplateResponse create(RecurringTemplateRequest request);
public RecurringTemplateResponse update(Long id, RecurringTemplateRequest request);
public void updateStatus(Long id, boolean active);
public void delete(Long id);
public RecurringGenerationResponse generate(YearMonth month);
```

Insert the generation marker and ledger row in one transaction. Treat the unique generation constraint as an already-generated result.

- [ ] **Step 5: Run recurring, ledger, and migration tests**

Expected: PASS.

- [ ] **Step 6: Commit**

`git commit -m "feat: add recurring household entries"`

### Task 5: Implement CSV preview, import, and export

**Files:**
- Create: `backend/src/main/java/com/family/finance/ledger/csv/CsvCodec.java`
- Create: `backend/src/main/java/com/family/finance/ledger/dto/EntryImportPreviewResponse.java`
- Create: `backend/src/main/java/com/family/finance/ledger/dto/EntryImportRequest.java`
- Create: `backend/src/main/java/com/family/finance/ledger/service/EntryCsvService.java`
- Create: `backend/src/test/java/com/family/finance/ledger/EntryCsvServiceTest.java`
- Modify: `backend/src/main/java/com/family/finance/ledger/web/LedgerController.java`
- Modify: `backend/src/main/java/com/family/finance/ledger/mapper/LedgerEntryMapper.java`

- [ ] **Step 1: Write failing parser and permission tests**

Cover UTF-8 BOM, quoted commas, escaped quotes, blank lines, Chinese and enum types, amount/date errors, inactive categories, member restrictions, 500-row limit, export escaping, and transactional rollback.

- [ ] **Step 2: Run the CSV test and verify failure**

- [ ] **Step 3: Implement a small RFC-4180-compatible codec**

The codec must parse quoted fields and doubled quotes without a new dependency and must produce a UTF-8 BOM on export for Excel compatibility.

- [ ] **Step 4: Implement preview and commit**

```java
public EntryImportPreviewResponse preview(String csv);
@Transactional public int commit(EntryImportRequest request);
public String export(EntryQuery query);
```

The commit payload contains the original CSV plus a preview checksum. Re-parse and revalidate on commit; reject stale or modified content.

- [ ] **Step 5: Add controller endpoints with `text/csv` export**

- [ ] **Step 6: Run CSV, ledger, and security tests**

Expected: PASS.

- [ ] **Step 7: Commit**

`git commit -m "feat: add ledger csv tools"`

### Task 6: Extend statistics and normalize 400 errors

**Files:**
- Modify: `backend/src/main/java/com/family/finance/statistics/dto/DashboardResponse.java`
- Modify: `backend/src/main/java/com/family/finance/statistics/service/StatisticsService.java`
- Modify: `backend/src/main/java/com/family/finance/common/error/ApiExceptionHandler.java`
- Create: `backend/src/test/java/com/family/finance/statistics/StatisticsServiceTest.java`
- Create: `backend/src/test/java/com/family/finance/common/error/ApiExceptionHandlerTest.java`

- [ ] **Step 1: Write failing statistics tests**

Assert previous-period comparison, null savings rate when income is zero, budget usage, anomaly threshold, and logical-delete filtering.

- [ ] **Step 2: Write failing web error tests**

Invalid dates, enum values, months, and pagination must return HTTP 400 with a stable code rather than 500.

- [ ] **Step 3: Extend the dashboard response**

Add `comparison`, `savingsRate`, `budget`, and `anomalies` fields while keeping all existing fields backward compatible.

- [ ] **Step 4: Implement calculations from ledger and budget tables**

Use `BigDecimal` with explicit scale and rounding. Do not store calculated aggregates.

- [ ] **Step 5: Normalize conversion exceptions**

Handle Spring request-binding and type-conversion exceptions as `INVALID_ARGUMENT` HTTP 400 without exposing stack traces.

- [ ] **Step 6: Run statistics, error, and existing backend tests**

Expected: PASS.

- [ ] **Step 7: Commit**

`git commit -m "feat: extend household statistics"`

### Task 7: Introduce the style-C frontend shell and lazy loading

**Files:**
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/main.ts`
- Modify: `frontend/src/layouts/AppLayout.vue`
- Modify: `frontend/src/styles/tokens.css`
- Split/modify: `frontend/src/styles/main.css`
- Create: `frontend/src/styles/monthly-report.css`
- Create: `frontend/src/styles/data-pages.css`
- Modify: `frontend/src/__tests__/router.spec.ts`
- Modify: `frontend/src/__tests__/router-guard.spec.ts`

- [ ] **Step 1: Update route tests for budgets, recurring, and activity routes**

- [ ] **Step 2: Convert business pages to dynamic imports**

Use `() => import('../views/DashboardView.vue')` for authenticated business views. Keep small auth pages eager only if it improves first-load behavior.

- [ ] **Step 3: Replace the shell visual system**

Implement rice-white background, ink header, cinnabar month rail, bamboo income, ultramarine balance, hairline rules, strong tabular numerals, desktop “more” menu, and mobile bottom navigation.

- [ ] **Step 4: Reduce full-library imports**

Remove `app.use(ElementPlus)`. Import only used Element Plus components or configure the existing Vite build for deterministic on-demand imports without adding a dependency.

- [ ] **Step 5: Run router tests and type check**

Expected: PASS.

- [ ] **Step 6: Commit**

`git commit -m "feat: introduce family report visual system"`

### Task 8: Fix frontend correctness and accessibility

**Files:**
- Modify: `frontend/src/components/EntryForm.vue`
- Modify: `frontend/src/components/TrendChart.vue`
- Modify: `frontend/src/components/StateBlock.vue`
- Modify: `frontend/src/views/EntriesView.vue`
- Modify: auth and onboarding forms
- Create: `frontend/src/utils/date.ts`
- Create: `frontend/src/__tests__/date.spec.ts`
- Modify: `frontend/src/__tests__/entry-form.spec.ts`

- [ ] **Step 1: Write date-boundary and same-route drawer tests**

Test local date formatting without `toISOString()` and repeated `?new=1` navigation.

- [ ] **Step 2: Implement local date utility and query watcher**

```ts
export function localDateString(date = new Date()) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
```

Watch `route.query.new`; open the drawer when it becomes `1` and remove the query on close.

- [ ] **Step 3: Add field-level errors and accessibility labels**

Associate errors with inputs, label icon-only actions, make loading state announce its title, and provide a textual trend summary.

- [ ] **Step 4: Replace low-contrast small text tokens**

Use the existing muted token or a darker value that reaches the 4.5:1 target on rice-white and white surfaces.

- [ ] **Step 5: Run focused frontend tests and type check**

Expected: PASS.

- [ ] **Step 6: Commit**

`git commit -m "fix: improve ledger frontend correctness"`

### Task 9: Add budget and recurring frontend flows

**Files:**
- Create: `frontend/src/api/budgets.ts`
- Create: `frontend/src/api/recurring.ts`
- Modify: `frontend/src/types/domain.ts`
- Create: `frontend/src/views/BudgetsView.vue`
- Create: `frontend/src/views/RecurringView.vue`
- Create: `frontend/src/components/BudgetProgress.vue`
- Create: `frontend/src/components/RecurringForm.vue`
- Create: `frontend/src/__tests__/budgets.spec.ts`
- Create: `frontend/src/__tests__/recurring.spec.ts`

- [ ] **Step 1: Write failing view tests for role visibility and core actions**

- [ ] **Step 2: Implement typed APIs and domain models**

- [ ] **Step 3: Implement budget month selection and parent editing**

Members see the same progress data without edit controls. Status always has text in addition to color.

- [ ] **Step 4: Implement recurring template management and generation result**

Show created, already generated, and skipped counts with skip reasons.

- [ ] **Step 5: Run focused tests and type check**

Expected: PASS.

- [ ] **Step 6: Commit**

`git commit -m "feat: add budget and recurring views"`

### Task 10: Add CSV and audit-log frontend flows

**Files:**
- Modify: `frontend/src/api/entries.ts`
- Create: `frontend/src/api/audit.ts`
- Create: `frontend/src/components/EntryImportDialog.vue`
- Modify: `frontend/src/views/EntriesView.vue`
- Create: `frontend/src/views/AuditLogsView.vue`
- Create: `frontend/src/__tests__/entry-import.spec.ts`
- Create: `frontend/src/__tests__/audit-logs.spec.ts`

- [ ] **Step 1: Write failing import, export, and parent-only log tests**

- [ ] **Step 2: Add file preview and download APIs**

Preserve the original CSV until commit, show row number and field errors, and revoke generated Blob URLs after download.

- [ ] **Step 3: Add parent-only audit timeline**

Render time, actor, action, object type, and server-authored summary with pagination.

- [ ] **Step 4: Run focused tests and type check**

Expected: PASS.

- [ ] **Step 5: Commit**

`git commit -m "feat: add csv and audit frontend"`

### Task 11: Redesign the dashboard as a family monthly report

**Files:**
- Modify: `frontend/src/api/statistics.ts`
- Modify: `frontend/src/types/domain.ts`
- Modify: `frontend/src/views/DashboardView.vue`
- Modify: `frontend/src/components/TrendChart.vue`
- Modify: `frontend/src/__tests__/dashboard.spec.ts`

- [ ] **Step 1: Extend dashboard tests**

Cover comparison labels, savings-rate empty state, budget states, anomaly messages, income/expense labels, and member comparison.

- [ ] **Step 2: Register only required ECharts modules**

Use `echarts/core`, `LineChart`, required components, and `CanvasRenderer`; remove `import * as echarts`.

- [ ] **Step 3: Implement the approved style-C hierarchy**

Use a dominant month marker and balance number, editorial KPI row, trend and category report, budget strip, member section, anomalies, and recent ledger. Keep one “记一笔” action.

- [ ] **Step 4: Add print styles for a clean monthly report**

Hide navigation and controls under `@media print`; preserve figures and tables without clipping.

- [ ] **Step 5: Run dashboard tests, type check, and production build**

Expected: PASS with route chunks and a smaller initial JavaScript bundle than 2,273,740 bytes.

- [ ] **Step 6: Commit**

`git commit -m "feat: redesign dashboard as monthly report"`

### Task 12: Final integration, documentation, and acceptance

**Files:**
- Modify: `README.md`
- Modify: `docs/01-PRD.md`
- Modify: `docs/02-架构设计.md`
- Modify: `docs/stages/05-测试验收与报告素材.md`
- Modify generated report content only if it can be regenerated without overwriting user identity data

- [ ] **Step 1: Run the backend authoritative gate**

Run: `backend\mvnw.cmd -f backend\pom.xml clean verify`

Expected: all tests pass, migration generation is not skipped, executable JAR builds.

- [ ] **Step 2: Run the frontend authoritative gate**

Run from `frontend`: `npm run type-check`, `npm run test:unit`, `npm run build`.

Expected: all commands exit 0; build shows route-level chunks.

- [ ] **Step 3: Start a dedicated acceptance environment**

Use an isolated MySQL database/container and non-conflicting ports. Do not access the user's unrelated databases.

- [ ] **Step 4: Run browser acceptance**

Verify parent and member flows for budgets, recurring generation, CSV preview/import/export, enhanced monthly report, audit permissions, repeated “记一笔”, invalid parameters, desktop layout, print view, and 390px mobile layout.

- [ ] **Step 5: Update documentation with actual evidence**

Record exact commands, test counts, build output sizes, browser versions, ports, and any remaining risk. Do not claim checks that were not run.

- [ ] **Step 6: Inspect the complete diff and working tree**

Check secrets, debug output, temporary files, generated artifacts, unrelated formatting, and all tracked/untracked files.

- [ ] **Step 7: Commit the verified milestone**

`git commit -m "docs: record monthly report acceptance"`
