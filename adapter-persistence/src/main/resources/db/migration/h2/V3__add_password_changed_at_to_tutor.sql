-- Variante H2 da V3 (TIMESTAMPTZ e TO_TIMESTAMP não existem no H2)
ALTER TABLE tutor
    ADD COLUMN password_changed_at TIMESTAMP WITH TIME ZONE;

UPDATE tutor
SET password_changed_at = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00'; -- epoch

ALTER TABLE tutor
    ALTER COLUMN password_changed_at SET DEFAULT NOW();
