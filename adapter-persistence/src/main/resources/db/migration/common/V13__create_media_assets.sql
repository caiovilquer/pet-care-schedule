CREATE TABLE media_asset (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    tutor_id BIGINT NULL,
    pet_id BIGINT NULL,
    purpose VARCHAR(32) NOT NULL,
    original_filename VARCHAR(180) NOT NULL,
    content_type VARCHAR(40) NOT NULL,
    expected_size BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    staging_key VARCHAR(512) NOT NULL UNIQUE,
    object_key VARCHAR(512) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ready_at TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT fk_media_tutor FOREIGN KEY (tutor_id) REFERENCES tutor(id) ON DELETE SET NULL,
    CONSTRAINT fk_media_pet FOREIGN KEY (pet_id) REFERENCES pet(id) ON DELETE SET NULL,
    CONSTRAINT ck_media_size CHECK (expected_size BETWEEN 1 AND 5242880),
    CONSTRAINT ck_media_content_type CHECK (content_type IN ('image/jpeg', 'image/png')),
    CONSTRAINT ck_media_purpose CHECK (purpose IN ('PET_PHOTO', 'TUTOR_AVATAR')),
    CONSTRAINT ck_media_status CHECK (status IN ('PENDING', 'READY', 'PENDING_DELETE', 'REJECTED')),
    CONSTRAINT ck_media_checksum_length CHECK (CHAR_LENGTH(checksum_sha256) = 64)
);

ALTER TABLE pet ADD COLUMN photo_asset_id UUID NULL;
ALTER TABLE tutor ADD COLUMN avatar_asset_id UUID NULL;

ALTER TABLE pet ADD CONSTRAINT fk_pet_photo_asset
    FOREIGN KEY (photo_asset_id) REFERENCES media_asset(id) ON DELETE SET NULL;
ALTER TABLE tutor ADD CONSTRAINT fk_tutor_avatar_asset
    FOREIGN KEY (avatar_asset_id) REFERENCES media_asset(id) ON DELETE SET NULL;

CREATE INDEX idx_media_tutor_status ON media_asset(tutor_id, status);
CREATE INDEX idx_media_pet ON media_asset(pet_id);
CREATE INDEX idx_media_cleanup ON media_asset(status, created_at);
