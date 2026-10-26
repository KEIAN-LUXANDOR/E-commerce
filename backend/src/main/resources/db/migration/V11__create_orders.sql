-- V11: customer orders and the diamonds in each order
CREATE SEQUENCE order_number_seq START WITH 100001;

CREATE TABLE orders (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number    VARCHAR(20)  NOT NULL UNIQUE,
    user_id         UUID         NOT NULL REFERENCES users(id),
    status          VARCHAR(20)  NOT NULL CHECK (status IN ('PENDING_PAYMENT','PAID','SHIPPED','DELIVERED','CANCELLED','REFUNDED')),
    subtotal_paise  BIGINT       NOT NULL CHECK (subtotal_paise >= 0),
    shipping_paise  BIGINT       NOT NULL CHECK (shipping_paise >= 0),
    discount_paise  BIGINT       NOT NULL CHECK (discount_paise >= 0),
    gst_paise       BIGINT       NOT NULL CHECK (gst_paise >= 0),
    total_paise     BIGINT       NOT NULL CHECK (total_paise >= 0),
    currency        VARCHAR(3)   NOT NULL,
    -- Copy of the delivery address at the time of ordering
    ship_full_name  VARCHAR(100) NOT NULL,
    ship_phone      VARCHAR(10)  NOT NULL,
    ship_line1      VARCHAR(200) NOT NULL,
    ship_line2      VARCHAR(200),
    ship_landmark   VARCHAR(100),
    ship_city       VARCHAR(80)  NOT NULL,
    ship_state      VARCHAR(60)  NOT NULL,
    ship_pincode    VARCHAR(6)   NOT NULL,
    expires_at      TIMESTAMPTZ  NOT NULL,
    paid_at         TIMESTAMPTZ,
    cancelled_at    TIMESTAMPTZ,
    cancel_reason   VARCHAR(30),
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT orders_total_chk CHECK (total_paise = subtotal_paise + shipping_paise + gst_paise - discount_paise)
);

CREATE INDEX orders_user_created_idx ON orders (user_id, created_at DESC);
CREATE INDEX orders_pending_expiry_idx ON orders (expires_at) WHERE status = 'PENDING_PAYMENT';
-- A customer can have only one unpaid order at a time
CREATE UNIQUE INDEX orders_one_pending_per_user ON orders (user_id) WHERE status = 'PENDING_PAYMENT';

CREATE TABLE order_items (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id      UUID          NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    line_number   INT           NOT NULL,
    product_id    UUID          REFERENCES products(id) ON DELETE SET NULL,
    variant_id    UUID          REFERENCES product_variants(id) ON DELETE SET NULL,
    product_slug  VARCHAR(200)  NOT NULL,
    product_name  VARCHAR(200)  NOT NULL,
    sku           VARCHAR(100)  NOT NULL,
    description   VARCHAR(200),
    image_url     VARCHAR(1000),
    price_paise   BIGINT        NOT NULL CHECK (price_paise >= 0),
    CONSTRAINT order_items_line_uk UNIQUE (order_id, line_number)
);