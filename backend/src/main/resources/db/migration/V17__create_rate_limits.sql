-- V17: "at most one email per minute" limits, stored in the database
-- so they survive restarts and work with more than one server.
CREATE TABLE rate_limits (
    limit_key  VARCHAR(320) PRIMARY KEY,   -- e.g. "reset:riya@example.com"
    last_at    TIMESTAMPTZ  NOT NULL       -- when the last email was allowed
);

CREATE INDEX rate_limits_last_at_idx ON rate_limits (last_at);