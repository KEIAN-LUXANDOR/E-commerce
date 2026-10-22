-- V8: products a customer has saved to their wishlist
CREATE TABLE wishlist_items (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id  UUID         NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT wishlist_items_user_product_uk UNIQUE (user_id, product_id)
);

CREATE INDEX wishlist_items_user_created_idx ON wishlist_items (user_id, created_at DESC);