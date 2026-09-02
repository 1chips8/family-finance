CREATE TABLE household (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL,
    invite_code CHAR(8) NOT NULL,
    created_by BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_household_invite_code (invite_code),
    KEY idx_household_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE app_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    household_id BIGINT NULL,
    username VARCHAR(32) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(40) NOT NULL,
    member_no VARCHAR(16) NULL,
    role VARCHAR(16) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_user_username (username),
    UNIQUE KEY uk_app_user_household_member_no (household_id, member_no),
    KEY idx_app_user_household_status (household_id, status),
    CONSTRAINT fk_app_user_household FOREIGN KEY (household_id) REFERENCES household (id) ON DELETE SET NULL,
    CONSTRAINT chk_app_user_role CHECK (role IS NULL OR role IN ('PARENT', 'MEMBER')),
    CONSTRAINT chk_app_user_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE household
    ADD CONSTRAINT fk_household_creator FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE RESTRICT;

CREATE TABLE finance_category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    household_id BIGINT NULL,
    scope VARCHAR(16) NOT NULL,
    type VARCHAR(16) NOT NULL,
    name VARCHAR(30) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_household_type_name (household_id, type, name),
    KEY idx_category_household_type_status (household_id, type, status),
    CONSTRAINT fk_category_household FOREIGN KEY (household_id) REFERENCES household (id) ON DELETE CASCADE,
    CONSTRAINT chk_category_scope CHECK (scope IN ('SYSTEM', 'CUSTOM')),
    CONSTRAINT chk_category_type CHECK (type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT chk_category_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_category_scope_owner CHECK ((scope = 'SYSTEM' AND household_id IS NULL) OR (scope = 'CUSTOM' AND household_id IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ledger_entry (
    id BIGINT NOT NULL AUTO_INCREMENT,
    household_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    type VARCHAR(16) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    occurred_on DATE NOT NULL,
    note VARCHAR(255) NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_entry_household_occurred (household_id, occurred_on, created_at),
    KEY idx_entry_household_member (household_id, member_id, occurred_on),
    KEY idx_entry_household_type (household_id, type, occurred_on),
    KEY idx_entry_household_category (household_id, category_id, occurred_on),
    CONSTRAINT fk_entry_household FOREIGN KEY (household_id) REFERENCES household (id) ON DELETE RESTRICT,
    CONSTRAINT fk_entry_member FOREIGN KEY (member_id) REFERENCES app_user (id) ON DELETE RESTRICT,
    CONSTRAINT fk_entry_category FOREIGN KEY (category_id) REFERENCES finance_category (id) ON DELETE RESTRICT,
    CONSTRAINT chk_entry_type CHECK (type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT chk_entry_amount CHECK (amount > 0),
    CONSTRAINT chk_entry_deleted CHECK (deleted IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO finance_category (scope, type, name, status)
SELECT 'SYSTEM', 'INCOME', '工资', 'ACTIVE'
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM finance_category WHERE scope = 'SYSTEM' AND type = 'INCOME' AND name = '工资'
);

INSERT INTO finance_category (scope, type, name, status)
SELECT 'SYSTEM', 'INCOME', '奖金', 'ACTIVE'
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM finance_category WHERE scope = 'SYSTEM' AND type = 'INCOME' AND name = '奖金'
);

INSERT INTO finance_category (scope, type, name, status)
SELECT 'SYSTEM', 'EXPENSE', '餐饮', 'ACTIVE'
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM finance_category WHERE scope = 'SYSTEM' AND type = 'EXPENSE' AND name = '餐饮'
);

INSERT INTO finance_category (scope, type, name, status)
SELECT 'SYSTEM', 'EXPENSE', '交通', 'ACTIVE'
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM finance_category WHERE scope = 'SYSTEM' AND type = 'EXPENSE' AND name = '交通'
);

INSERT INTO finance_category (scope, type, name, status)
SELECT 'SYSTEM', 'EXPENSE', '住房', 'ACTIVE'
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM finance_category WHERE scope = 'SYSTEM' AND type = 'EXPENSE' AND name = '住房'
);

INSERT INTO finance_category (scope, type, name, status)
SELECT 'SYSTEM', 'EXPENSE', '生活缴费', 'ACTIVE'
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM finance_category WHERE scope = 'SYSTEM' AND type = 'EXPENSE' AND name = '生活缴费'
);
