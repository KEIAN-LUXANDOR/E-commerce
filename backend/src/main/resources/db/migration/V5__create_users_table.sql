-- V5: customer and admin accounts
CREATE TABLE users (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    full_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(15),
    role            VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER',
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT users_role_chk   CHECK (role   IN ('CUSTOMER', 'ADMIN')),
    CONSTRAINT users_status_chk CHECK (status IN ('ACTIVE', 'DISABLED'))
);

-- Same email in different capital letters counts as a duplicate
CREATE UNIQUE INDEX users_email_lower_uk ON users (LOWER(email));