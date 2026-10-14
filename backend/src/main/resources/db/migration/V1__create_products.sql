CREATE TABLE products (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    slug        VARCHAR(200)  NOT NULL UNIQUE,
    name        VARCHAR(200)  NOT NULL,
    description VARCHAR(5000),
    status      VARCHAR(20)   NOT NULL DEFAULT 'DRAFT'
                CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);