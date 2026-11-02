-- V18: wrong-password counter per email, stored in the database
-- so a lock survives restarts and works with more than one server.
-- 5 wrong passwords within 15 minutes (from the first one) = locked until those 15 minutes are over.
CREATE TABLE login_attempts (
    email          VARCHAR(320) PRIMARY KEY,   -- always lower case
    failures       INT          NOT NULL CHECK (failures >= 1),
    first_failure  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX login_attempts_first_failure_idx ON login_attempts (first_failure);