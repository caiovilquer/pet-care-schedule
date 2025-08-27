
ALTER TABLE tutor
    ADD COLUMN password_changed_at TIMESTAMPTZ;

UPDATE tutor
SET password_changed_at = TO_TIMESTAMP(0); -- epoch

ALTER TABLE tutor
    ALTER COLUMN password_changed_at SET DEFAULT NOW();
