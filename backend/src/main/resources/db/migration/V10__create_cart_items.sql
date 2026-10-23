-- V10: each customer's shopping cart (one row per diamond)
CREATE TABLE cart_items (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    variant_id  UUID         NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT cart_items_user_variant_uk UNIQUE (user_id, variant_id)
);

CREATE INDEX cart_items_user_created_idx ON cart_items (user_id, created_at DESC);