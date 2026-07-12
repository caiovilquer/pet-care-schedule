-- Registros sem proprietário não são alcançáveis pela API e violam as
-- invariantes do domínio. Remove eventuais órfãos legados antes de endurecer
-- as constraints para que a migração seja segura em bases existentes.
DELETE FROM event
 WHERE pet_id IS NULL
    OR NOT EXISTS (SELECT 1 FROM pet p WHERE p.id = event.pet_id);

DELETE FROM pet
 WHERE tutor_id IS NULL
    OR NOT EXISTS (SELECT 1 FROM tutor t WHERE t.id = pet.tutor_id);

DELETE FROM refresh_token
 WHERE NOT EXISTS (SELECT 1 FROM tutor t WHERE t.id = refresh_token.user_id);

ALTER TABLE pet ALTER COLUMN tutor_id SET NOT NULL;
ALTER TABLE event ALTER COLUMN pet_id SET NOT NULL;

ALTER TABLE refresh_token
    ADD CONSTRAINT fk_refresh_token_user
    FOREIGN KEY (user_id) REFERENCES tutor(id)
    ON DELETE CASCADE;

ALTER TABLE tutor ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE pet ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE event ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_pet_tutor ON pet(tutor_id);
CREATE INDEX IF NOT EXISTS idx_event_pet ON event(pet_id);
CREATE INDEX IF NOT EXISTS idx_event_status_date ON event(status, date_start);
CREATE INDEX IF NOT EXISTS idx_event_pet_date ON event(pet_id, date_start);
