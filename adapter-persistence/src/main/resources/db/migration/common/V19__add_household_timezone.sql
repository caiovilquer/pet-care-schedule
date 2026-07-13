-- Existing rows intentionally remain NULL: the domain fallback preserves their
-- historical America/Sao_Paulo wall-clock meaning without a destructive backfill.
ALTER TABLE household ADD COLUMN timezone VARCHAR(64);

-- Legacy Event reminders keep their resolved household zone in the outbox so
-- retries format the same wall-clock even if delivery happens later.
ALTER TABLE reminder_outbox ADD COLUMN timezone VARCHAR(64);
