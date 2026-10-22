-- DEV ONLY: sample diamonds for testing the shop filters.
-- Never put this file in db/migration. Safe to run again (it replaces the samples).
BEGIN;

-- Remove the old non-diamond test products and any earlier samples
DELETE FROM product_images   WHERE product_id IN (SELECT id FROM products WHERE slug IN ('cotton-tshirt','steel-bottle','draft-item') OR slug LIKE 'sample-%');
DELETE FROM product_variants WHERE product_id IN (SELECT id FROM products WHERE slug IN ('cotton-tshirt','steel-bottle','draft-item') OR slug LIKE 'sample-%');
DELETE FROM products         WHERE slug IN ('cotton-tshirt','steel-bottle','draft-item') OR slug LIKE 'sample-%';

CREATE TEMP TABLE sample_data (
    slug text, name text, shape text, carat numeric, color text, clarity text, cut text,
    polish text, symmetry text, fluorescence text, origin text, lab text,
    quick_ship boolean, price_paise bigint, has_image boolean, product_id uuid DEFAULT gen_random_uuid()
) ON COMMIT DROP;

INSERT INTO sample_data (slug, name, shape, carat, color, clarity, cut, polish, symmetry, fluorescence, origin, lab, quick_ship, price_paise, has_image) VALUES
('sample-emerald-201-lab',     '2.01 ct Emerald Lab-grown Diamond', 'EMERALD',  2.01, 'F', 'VS1',  NULL,        'EXCELLENT', 'EXCELLENT', 'NONE',   'LAB_GROWN', 'IGI', true,  19500000, true),
('sample-emerald-230-lab',     '2.30 ct Emerald Lab-grown Diamond', 'EMERALD',  2.30, 'G', 'VVS2', NULL,        'VERY_GOOD', 'EXCELLENT', 'NONE',   'LAB_GROWN', 'IGI', false, 21000000, true),
('sample-emerald-245-lab',     '2.45 ct Emerald Lab-grown Diamond', 'EMERALD',  2.45, 'E', 'VS2',  NULL,        'EXCELLENT', 'VERY_GOOD', 'FAINT',  'LAB_GROWN', 'IGI', true,  23500000, false),
('sample-emerald-150-natural', '1.50 ct Emerald Natural Diamond',   'EMERALD',  1.50, 'H', 'VS1',  NULL,        'EXCELLENT', 'EXCELLENT', 'NONE',   'NATURAL',   'GIA', false, 92000000, true),
('sample-round-100-lab',       '1.00 ct Round Lab-grown Diamond',   'ROUND',    1.00, 'D', 'VVS1', 'IDEAL',     'EXCELLENT', 'EXCELLENT', 'NONE',   'LAB_GROWN', 'IGI', true,   9500000, true),
('sample-round-205-natural',   '2.05 ct Round Natural Diamond',     'ROUND',    2.05, 'G', 'VS2',  'EXCELLENT', 'EXCELLENT', 'EXCELLENT', 'MEDIUM', 'NATURAL',   'GIA', false, 310000000, true),
('sample-oval-102-lab',        '1.02 ct Oval Lab-grown Diamond',    'OVAL',     1.02, 'F', 'VS1',  NULL,        'EXCELLENT', 'EXCELLENT', 'NONE',   'LAB_GROWN', 'IGI', true,  18500000, true),
('sample-pear-120-lab',        '1.20 ct Pear Lab-grown Diamond',    'PEAR',     1.20, 'E', 'VVS2', NULL,        'VERY_GOOD', 'VERY_GOOD', 'NONE',   'LAB_GROWN', 'IGI', false, 11000000, true),
('sample-cushion-180-natural', '1.80 ct Cushion Natural Diamond',   'CUSHION',  1.80, 'I', 'SI1',  NULL,        'GOOD',      'VERY_GOOD', 'STRONG', 'NATURAL',   'GIA', false, 95000000, true),
('sample-princess-075-natural','0.75 ct Princess Natural Diamond',  'PRINCESS', 0.75, 'G', 'VS2',  NULL,        'EXCELLENT', 'VERY_GOOD', 'FAINT',  'NATURAL',   'IGI', true,  21000000, false),
('sample-radiant-154-lab',     '1.54 ct Radiant Lab-grown Diamond', 'RADIANT',  1.54, 'F', 'VS1',  NULL,        'EXCELLENT', 'EXCELLENT', 'NONE',   'LAB_GROWN', 'IGI', true,  14000000, true),
('sample-heart-110-lab',       '1.10 ct Heart Lab-grown Diamond',   'HEART',    1.10, 'G', 'SI1',  NULL,        'GOOD',      'GOOD',      'NONE',   'LAB_GROWN', 'IGI', false,  7500000, true);

INSERT INTO products (id, slug, name, description, status)
SELECT product_id, slug, name, 'Sample diamond for testing the shop filters.', 'ACTIVE' FROM sample_data;

INSERT INTO diamond_details (product_id, origin, shape, carat, color, clarity, cut, polish, symmetry, fluorescence, lab, quick_ship)
SELECT product_id, origin, shape, carat, color, clarity, cut, polish, symmetry, fluorescence, lab, quick_ship FROM sample_data;

INSERT INTO product_variants (id, product_id, sku, name, price_paise, mrp_paise, stock_quantity, active, sort_order)
SELECT gen_random_uuid(), product_id, upper(slug), 'Single stone', price_paise, NULL, 1, true, 0 FROM sample_data;

INSERT INTO product_images (id, product_id, url, alt_text, sort_order)
SELECT gen_random_uuid(), product_id, 'https://placehold.co/800x800?text=' || initcap(shape), name, 0
FROM sample_data WHERE has_image;

COMMIT;

SELECT p.name, d.origin, d.shape, d.carat, d.color, d.clarity, d.quick_ship,
       (SELECT count(*) FROM product_images i WHERE i.product_id = p.id) AS images
FROM products p JOIN diamond_details d ON d.product_id = p.id
ORDER BY d.origin, d.shape, d.carat;