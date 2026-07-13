ALTER TABLE household_invitation DROP CONSTRAINT ck_household_invite_role;

ALTER TABLE household_invitation ADD CONSTRAINT ck_household_invite_role
    CHECK (role IN ('OWNER', 'CAREGIVER', 'VIEWER'));
