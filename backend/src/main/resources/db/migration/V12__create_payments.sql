-- V12: payment attempts with Razorpay (Razorpay needs a new order for every attempt)
CREATE TABLE payments (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id             UUID         NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    attempt_number       INT          NOT NULL CHECK (attempt_number > 0),
    provider             VARCHAR(20)  NOT NULL,
    provider_order_id    VARCHAR(50)  NOT NULL UNIQUE,
    provider_payment_id  VARCHAR(50)  UNIQUE,
    amount_paise         BIGINT       NOT NULL CHECK (amount_paise >= 100),
    currency             VARCHAR(3)   NOT NULL,
    status               VARCHAR(20)  NOT NULL CHECK (status IN ('CREATED','PAID','REFUND_REQUIRED')),
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,
    CONSTRAINT payments_order_attempt_uk UNIQUE (order_id, attempt_number)
);

CREATE INDEX payments_order_idx ON payments (order_id);
-- An order can be paid successfully only once
CREATE UNIQUE INDEX payments_one_paid_per_order ON payments (order_id) WHERE status = 'PAID';