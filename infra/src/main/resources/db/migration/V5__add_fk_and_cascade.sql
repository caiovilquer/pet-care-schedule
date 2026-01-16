ALTER TABLE pet
    DROP CONSTRAINT IF EXISTS pet_tutor_id_fkey;

ALTER TABLE pet
    ADD CONSTRAINT fk_pet_tutor
    FOREIGN KEY (tutor_id) REFERENCES tutor(id)
    ON DELETE CASCADE;

ALTER TABLE event
    DROP CONSTRAINT IF EXISTS event_pet_id_fkey;

ALTER TABLE event
    ADD CONSTRAINT fk_event_pet
    FOREIGN KEY (pet_id) REFERENCES pet(id)
    ON DELETE CASCADE;

ALTER TABLE password_reset_token
    DROP CONSTRAINT IF EXISTS fk_password_reset_token_user;

ALTER TABLE password_reset_token
    ADD CONSTRAINT fk_password_reset_token_user
    FOREIGN KEY (user_id) REFERENCES tutor(id)
    ON DELETE CASCADE;
