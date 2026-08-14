ALTER TABLE refresh_tokens
    ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMPTZ;

ALTER TABLE refresh_tokens
    ADD COLUMN IF NOT EXISTS replaced_by_token_id UUID;