CREATE TABLE monthly_budget (
    id BIGINT NOT NULL AUTO_INCREMENT,
    household_id BIGINT NOT NULL,
    budget_month DATE NOT NULL,
    category_id BIGINT NULL,
    category_key BIGINT GENERATED ALWAYS AS (IFNULL(category_id, 0)) STORED,
    amount DECIMAL(12,2) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_budget_household_month_category (household_id, budget_month, category_key),
    KEY idx_budget_household_month (household_id, budget_month),
    KEY idx_budget_category (category_id),
    CONSTRAINT fk_budget_household FOREIGN KEY (household_id) REFERENCES household (id) ON DELETE CASCADE,
    CONSTRAINT fk_budget_category FOREIGN KEY (category_id) REFERENCES finance_category (id) ON DELETE RESTRICT,
    CONSTRAINT chk_budget_month_start CHECK (DAY(budget_month) = 1),
    CONSTRAINT chk_budget_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE recurring_template (
    id BIGINT NOT NULL AUTO_INCREMENT,
    household_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    type VARCHAR(16) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    day_of_month TINYINT UNSIGNED NOT NULL,
    note VARCHAR(255) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_recurring_household_status (household_id, status),
    KEY idx_recurring_member (member_id),
    KEY idx_recurring_category (category_id),
    CONSTRAINT fk_recurring_household FOREIGN KEY (household_id) REFERENCES household (id) ON DELETE CASCADE,
    CONSTRAINT fk_recurring_member FOREIGN KEY (member_id) REFERENCES app_user (id) ON DELETE RESTRICT,
    CONSTRAINT fk_recurring_category FOREIGN KEY (category_id) REFERENCES finance_category (id) ON DELETE RESTRICT,
    CONSTRAINT chk_recurring_type CHECK (type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT chk_recurring_amount CHECK (amount > 0),
    CONSTRAINT chk_recurring_day CHECK (day_of_month BETWEEN 1 AND 31),
    CONSTRAINT chk_recurring_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE recurring_generation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recurring_template_id BIGINT NOT NULL,
    generated_month DATE NOT NULL,
    ledger_entry_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_recurring_generation_month (recurring_template_id, generated_month),
    KEY idx_generation_entry (ledger_entry_id),
    CONSTRAINT fk_generation_template FOREIGN KEY (recurring_template_id) REFERENCES recurring_template (id) ON DELETE CASCADE,
    CONSTRAINT fk_generation_entry FOREIGN KEY (ledger_entry_id) REFERENCES ledger_entry (id) ON DELETE RESTRICT,
    CONSTRAINT chk_generation_month_start CHECK (DAY(generated_month) = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    household_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    action VARCHAR(40) NOT NULL,
    object_type VARCHAR(40) NOT NULL,
    object_id BIGINT NULL,
    summary VARCHAR(255) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_audit_household_created (household_id, created_at),
    KEY idx_audit_actor (actor_id),
    CONSTRAINT fk_audit_household FOREIGN KEY (household_id) REFERENCES household (id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES app_user (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
