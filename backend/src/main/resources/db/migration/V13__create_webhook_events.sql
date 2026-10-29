-- V13: Razorpay webhook events already processed (Razorpay may send the same event more than once)
CREATE TABLE webhook_events (
    event_id     VARCHAR(100) PRIMARY KEY,
    provider     VARCHAR(20)  NOT NULL,
    event_type   VARCHAR(60)  NOT NULL,
    received_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);