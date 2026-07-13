CREATE TABLE health_record (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    tutor_id BIGINT NOT NULL,
    pet_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    title VARCHAR(120) NOT NULL,
    notes VARCHAR(4000),
    product_name VARCHAR(160),
    dosage VARCHAR(120),
    batch_number VARCHAR(120),
    professional_name VARCHAR(160),
    clinic_name VARCHAR(160),
    cost_amount NUMERIC(12, 2),
    currency VARCHAR(3),
    created_by_tutor_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_health_record_tutor FOREIGN KEY (tutor_id) REFERENCES tutor(id) ON DELETE CASCADE,
    CONSTRAINT fk_health_record_pet FOREIGN KEY (pet_id) REFERENCES pet(id) ON DELETE CASCADE,
    CONSTRAINT fk_health_record_author FOREIGN KEY (created_by_tutor_id) REFERENCES tutor(id),
    CONSTRAINT ck_health_record_type CHECK (type IN ('VACCINE', 'MEDICATION', 'CONSULTATION', 'EXAM', 'SYMPTOM', 'DAILY_CARE')),
    CONSTRAINT ck_health_record_title CHECK (CHAR_LENGTH(title) BETWEEN 1 AND 120),
    CONSTRAINT ck_health_record_cost CHECK (cost_amount IS NULL OR cost_amount BETWEEN 0 AND 9999999999.99),
    CONSTRAINT ck_health_record_currency CHECK (
        (cost_amount IS NULL AND currency IS NULL)
        OR (cost_amount IS NOT NULL AND CHAR_LENGTH(currency) = 3)
    ),
    CONSTRAINT ck_health_record_product_fields CHECK (
        (product_name IS NULL AND dosage IS NULL AND batch_number IS NULL)
        OR type IN ('VACCINE', 'MEDICATION')
    ),
    CONSTRAINT ck_health_record_professional_fields CHECK (
        (professional_name IS NULL AND clinic_name IS NULL)
        OR type IN ('VACCINE', 'MEDICATION', 'CONSULTATION', 'EXAM')
    )
);

CREATE TABLE health_measurement (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    tutor_id BIGINT NOT NULL,
    pet_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    measurement_value NUMERIC(10, 3) NOT NULL,
    unit VARCHAR(24) NOT NULL,
    measured_at TIMESTAMP WITH TIME ZONE NOT NULL,
    notes VARCHAR(500),
    created_by_tutor_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_health_measurement_tutor FOREIGN KEY (tutor_id) REFERENCES tutor(id) ON DELETE CASCADE,
    CONSTRAINT fk_health_measurement_pet FOREIGN KEY (pet_id) REFERENCES pet(id) ON DELETE CASCADE,
    CONSTRAINT fk_health_measurement_author FOREIGN KEY (created_by_tutor_id) REFERENCES tutor(id),
    CONSTRAINT ck_health_measurement_type CHECK (type IN ('WEIGHT', 'TEMPERATURE', 'BODY_CONDITION_SCORE')),
    CONSTRAINT ck_health_measurement_unit CHECK (unit IN ('KILOGRAM', 'CELSIUS', 'SCORE_1_TO_9')),
    CONSTRAINT ck_health_measurement_type_unit CHECK (
        (type = 'WEIGHT' AND unit = 'KILOGRAM' AND measurement_value BETWEEN 0.01 AND 500)
        OR (type = 'TEMPERATURE' AND unit = 'CELSIUS' AND measurement_value BETWEEN 20 AND 50)
        OR (type = 'BODY_CONDITION_SCORE' AND unit = 'SCORE_1_TO_9' AND measurement_value BETWEEN 1 AND 9 AND measurement_value = FLOOR(measurement_value))
    )
);

ALTER TABLE media_asset ADD COLUMN health_record_id UUID;
ALTER TABLE media_asset ADD CONSTRAINT fk_media_health_record
    FOREIGN KEY (health_record_id) REFERENCES health_record(id) ON DELETE SET NULL;

ALTER TABLE media_asset DROP CONSTRAINT ck_media_size;
ALTER TABLE media_asset ADD CONSTRAINT ck_media_size CHECK (expected_size BETWEEN 1 AND 10485760);
ALTER TABLE media_asset DROP CONSTRAINT ck_media_content_type;
ALTER TABLE media_asset ADD CONSTRAINT ck_media_content_type
    CHECK (content_type IN ('image/jpeg', 'image/png', 'application/pdf'));
ALTER TABLE media_asset DROP CONSTRAINT ck_media_purpose;
ALTER TABLE media_asset ADD CONSTRAINT ck_media_purpose
    CHECK (purpose IN ('PET_PHOTO', 'TUTOR_AVATAR', 'HEALTH_ATTACHMENT'));

CREATE TABLE health_record_attachment (
    id UUID PRIMARY KEY,
    health_record_id UUID NOT NULL,
    media_asset_id UUID NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_health_attachment_record FOREIGN KEY (health_record_id) REFERENCES health_record(id) ON DELETE CASCADE,
    CONSTRAINT fk_health_attachment_media FOREIGN KEY (media_asset_id) REFERENCES media_asset(id) ON DELETE CASCADE,
    CONSTRAINT uk_health_attachment_record_media UNIQUE (health_record_id, media_asset_id)
);

CREATE INDEX idx_health_record_pet_occurred ON health_record(pet_id, occurred_at DESC, id);
CREATE INDEX idx_health_record_tutor_occurred ON health_record(tutor_id, occurred_at DESC, id);
CREATE INDEX idx_health_record_type_occurred ON health_record(type, occurred_at DESC);
CREATE INDEX idx_health_measurement_pet_type_time ON health_measurement(pet_id, type, measured_at, id);
CREATE INDEX idx_health_measurement_tutor_time ON health_measurement(tutor_id, measured_at DESC);
CREATE INDEX idx_health_attachment_record ON health_record_attachment(health_record_id, created_at);
CREATE INDEX idx_media_health_record ON media_asset(health_record_id, status);
