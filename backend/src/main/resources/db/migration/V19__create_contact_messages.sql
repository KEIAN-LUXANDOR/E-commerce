-- V19: messages sent with the "Contact us" form (shown on the admin dashboard)
CREATE TABLE contact_messages (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID          REFERENCES users(id) ON DELETE SET NULL,   -- set when the sender was logged in
    name          VARCHAR(100)  NOT NULL,
    email         VARCHAR(255)  NOT NULL,
    phone         VARCHAR(25),
    subject       VARCHAR(150),
    order_number  VARCHAR(20),
    message       VARCHAR(5000) NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'NEW' CHECK (status IN ('NEW', 'HANDLED')),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    handled_at    TIMESTAMPTZ
);

CREATE INDEX contact_messages_status_created_idx ON contact_messages (status, created_at DESC);