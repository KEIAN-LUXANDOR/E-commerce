-- V6: one-time codes for email verification and password reset.
-- Only a SHA-256 hash of each code is stored, never the code itself.
CREATE TABLE user_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        VARCHAR(30)  NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT user_tokens_type_chk CHECK (type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET'))
);

CREATE UNIQUE INDEX user_tokens_hash_uk ON user_tokens (token_hash);
CREATE INDEX user_tokens_user_type_idx ON user_tokens (user_id, type);