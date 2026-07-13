-- Ciclo 5: resumo veterinário compartilhável e planejamento financeiro.
-- A migração é expansiva; custos previstos são snapshots opcionais.
ALTER TABLE care_plan ADD COLUMN estimated_cost_amount DECIMAL(12, 2);
ALTER TABLE care_plan ADD COLUMN estimated_cost_currency VARCHAR(3);
ALTER TABLE care_plan ADD CONSTRAINT ck_care_plan_estimated_cost CHECK (
    (estimated_cost_amount IS NULL AND estimated_cost_currency IS NULL)
    OR (estimated_cost_amount > 0 AND estimated_cost_amount <= 9999999999.99 AND CHAR_LENGTH(estimated_cost_currency) = 3)
);

ALTER TABLE care_occurrence ADD COLUMN estimated_cost_amount DECIMAL(12, 2);
ALTER TABLE care_occurrence ADD COLUMN estimated_cost_currency VARCHAR(3);
ALTER TABLE care_occurrence ADD CONSTRAINT ck_care_occurrence_estimated_cost CHECK (
    (estimated_cost_amount IS NULL AND estimated_cost_currency IS NULL)
    OR (estimated_cost_amount > 0 AND estimated_cost_amount <= 9999999999.99 AND CHAR_LENGTH(estimated_cost_currency) = 3)
);

CREATE TABLE expense (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    household_id UUID NOT NULL,
    pet_id BIGINT NOT NULL,
    category VARCHAR(24) NOT NULL,
    description VARCHAR(160) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    notes VARCHAR(1000),
    created_by_tutor_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_expense_household FOREIGN KEY (household_id) REFERENCES household(id) ON DELETE CASCADE,
    CONSTRAINT fk_expense_pet FOREIGN KEY (pet_id) REFERENCES pet(id) ON DELETE CASCADE,
    CONSTRAINT fk_expense_creator FOREIGN KEY (created_by_tutor_id) REFERENCES tutor(id),
    CONSTRAINT ck_expense_category CHECK (category IN ('VETERINARY', 'MEDICATION', 'VACCINE', 'EXAM', 'FOOD', 'HYGIENE', 'SERVICE', 'INSURANCE', 'OTHER')),
    CONSTRAINT ck_expense_description CHECK (CHAR_LENGTH(description) BETWEEN 1 AND 160),
    CONSTRAINT ck_expense_amount CHECK (amount > 0 AND amount <= 9999999999.99),
    CONSTRAINT ck_expense_currency CHECK (CHAR_LENGTH(currency) = 3 AND currency = UPPER(currency)),
    CONSTRAINT ck_expense_notes CHECK (notes IS NULL OR CHAR_LENGTH(notes) <= 1000)
);

CREATE TABLE veterinary_share (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    household_id UUID NOT NULL,
    pet_id BIGINT NOT NULL,
    created_by_tutor_id BIGINT NOT NULL,
    label VARCHAR(100) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    period_from DATE NOT NULL,
    period_to DATE NOT NULL,
    include_notes BOOLEAN NOT NULL DEFAULT FALSE,
    include_costs BOOLEAN NOT NULL DEFAULT FALSE,
    include_documents BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_accessed_at TIMESTAMP WITH TIME ZONE,
    access_count BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_veterinary_share_household FOREIGN KEY (household_id) REFERENCES household(id) ON DELETE CASCADE,
    CONSTRAINT fk_veterinary_share_pet FOREIGN KEY (pet_id) REFERENCES pet(id) ON DELETE CASCADE,
    CONSTRAINT fk_veterinary_share_creator FOREIGN KEY (created_by_tutor_id) REFERENCES tutor(id),
    CONSTRAINT ck_veterinary_share_label CHECK (CHAR_LENGTH(label) BETWEEN 1 AND 100),
    CONSTRAINT ck_veterinary_share_hash CHECK (CHAR_LENGTH(token_hash) = 64),
    CONSTRAINT ck_veterinary_share_period CHECK (period_to >= period_from),
    CONSTRAINT ck_veterinary_share_expiry CHECK (expires_at > created_at),
    CONSTRAINT ck_veterinary_share_access_count CHECK (access_count >= 0)
);

CREATE INDEX idx_expense_household_date ON expense(household_id, occurred_at DESC, id);
CREATE INDEX idx_expense_household_pet_date ON expense(household_id, pet_id, occurred_at DESC);
CREATE INDEX idx_care_occurrence_forecast ON care_occurrence(household_id, status, due_at, estimated_cost_currency);
CREATE INDEX idx_veterinary_share_household ON veterinary_share(household_id, created_at DESC);
CREATE INDEX idx_veterinary_share_pet_active ON veterinary_share(pet_id, revoked_at, expires_at);
