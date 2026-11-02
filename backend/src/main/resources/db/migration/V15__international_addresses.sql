-- V15: delivery addresses from any country (customers are outside India).
-- Existing addresses and orders are Indian: they get country IN and a +91 phone.

-- ---------- Saved addresses ----------
ALTER TABLE addresses DROP CONSTRAINT addresses_pincode_chk;
ALTER TABLE addresses DROP CONSTRAINT addresses_phone_chk;

ALTER TABLE addresses ADD COLUMN country VARCHAR(2) NOT NULL DEFAULT 'IN';
ALTER TABLE addresses ALTER COLUMN country DROP DEFAULT;

-- Phone is stored with its country code: +14155550100, +919876543210
ALTER TABLE addresses ALTER COLUMN phone TYPE VARCHAR(16);
UPDATE addresses SET phone = '+91' || phone WHERE phone NOT LIKE '+%';

-- Many countries have no state, and some have no postal code
ALTER TABLE addresses ALTER COLUMN state TYPE VARCHAR(100);
ALTER TABLE addresses ALTER COLUMN state DROP NOT NULL;
ALTER TABLE addresses ALTER COLUMN pincode TYPE VARCHAR(10);
ALTER TABLE addresses ALTER COLUMN pincode DROP NOT NULL;

ALTER TABLE addresses ADD CONSTRAINT addresses_country_chk CHECK (country ~ '^[A-Z]{2}$');
ALTER TABLE addresses ADD CONSTRAINT addresses_phone_chk CHECK (phone ~ '^\+[1-9][0-9]{6,14}$');
ALTER TABLE addresses ADD CONSTRAINT addresses_pincode_chk
    CHECK (pincode IS NULL OR pincode ~ '^[A-Z0-9][A-Z0-9 -]{1,8}[A-Z0-9]$');

-- ---------- Delivery address copied into each order ----------
ALTER TABLE orders ADD COLUMN ship_country VARCHAR(2) NOT NULL DEFAULT 'IN';
ALTER TABLE orders ALTER COLUMN ship_country DROP DEFAULT;

ALTER TABLE orders ALTER COLUMN ship_phone TYPE VARCHAR(16);
UPDATE orders SET ship_phone = '+91' || ship_phone WHERE ship_phone NOT LIKE '+%';

ALTER TABLE orders ALTER COLUMN ship_state TYPE VARCHAR(100);
ALTER TABLE orders ALTER COLUMN ship_state DROP NOT NULL;
ALTER TABLE orders ALTER COLUMN ship_pincode TYPE VARCHAR(10);
ALTER TABLE orders ALTER COLUMN ship_pincode DROP NOT NULL;

ALTER TABLE orders ADD CONSTRAINT orders_ship_country_chk CHECK (ship_country ~ '^[A-Z]{2}$');