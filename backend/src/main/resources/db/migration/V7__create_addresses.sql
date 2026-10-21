-- V7: delivery addresses saved by customers
CREATE TABLE addresses (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    full_name   VARCHAR(100) NOT NULL,
    phone       VARCHAR(10)  NOT NULL,
    line1       VARCHAR(200) NOT NULL,
    line2       VARCHAR(200),
    landmark    VARCHAR(100),
    city        VARCHAR(80)  NOT NULL,
    state       VARCHAR(60)  NOT NULL,
    pincode     VARCHAR(6)   NOT NULL,
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT addresses_pincode_chk CHECK (pincode ~ '^[1-9][0-9]{5}$'),
    CONSTRAINT addresses_phone_chk   CHECK (phone ~ '^[6-9][0-9]{9}$')
);

CREATE INDEX addresses_user_idx ON addresses (user_id);

-- Only one default address per customer
CREATE UNIQUE INDEX addresses_one_default_per_user ON addresses (user_id) WHERE is_default;