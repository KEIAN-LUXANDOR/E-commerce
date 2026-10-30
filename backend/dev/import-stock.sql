-- =====================================================================================
-- Import diamonds from the supplier stock sheet (Excel saved as "CSV UTF-8").
--
-- PREVIEW (nothing saved):
--   psql -v rate=88 -v markup=2 -f /tmp/import-stock.sql
-- SAVE:
--   psql -v rate=88 -v markup=2 -v apply=1 -f /tmp/import-stock.sql
--
-- rate     = rupees per US dollar          (required)
-- markup   = selling price / cost          (required, e.g. 2 = double the cost)
-- round_to = round selling price to this many rupees (optional, default 100)
--
-- Selling price (INR) = Total Price (USD) x rate x markup, rounded.
-- If "Total Price" is empty, Price (per carat) x Weight is used.
-- Re-running with a newer sheet UPDATES stones by Stock # (price, availability, grades, photos).
-- It never changes stock of stones already sold or reserved on our website.
-- =====================================================================================
\set ON_ERROR_STOP on
\pset footer off

\if :{?rate}
\else
\echo 'ERROR: give the exchange rate, e.g.  -v rate=88'
\quit
\endif
\if :{?markup}
\else
\echo 'ERROR: give the markup, e.g.  -v markup=2'
\quit
\endif
\if :{?round_to}
\else
\set round_to 100
\endif

BEGIN;

-- ---------- 1. Load the file exactly as it is ----------
CREATE TEMP TABLE stock_raw (
    line_no serial,
    c1 text,
    c2 text,
    c3 text,
    c4 text,
    c5 text,
    c6 text,
    c7 text,
    c8 text,
    c9 text,
    c10 text,
    c11 text,
    c12 text,
    c13 text,
    c14 text,
    c15 text,
    c16 text,
    c17 text,
    c18 text,
    c19 text,
    c20 text,
    c21 text,
    c22 text,
    c23 text,
    c24 text,
    c25 text,
    c26 text,
    c27 text,
    c28 text,
    c29 text,
    c30 text,
    c31 text,
    c32 text,
    c33 text,
    c34 text,
    c35 text,
    c36 text,
    c37 text,
    c38 text,
    c39 text,
    c40 text,
    c41 text,
    c42 text,
    c43 text,
    c44 text,
    c45 text,
    c46 text,
    c47 text,
    c48 text,
    c49 text,
    c50 text,
    c51 text,
    c52 text,
    c53 text,
    c54 text,
    c55 text,
    c56 text,
    c57 text,
    c58 text,
    c59 text,
    c60 text,
    c61 text,
    c62 text,
    c63 text,
    c64 text,
    c65 text,
    c66 text,
    c67 text,
    c68 text,
    c69 text,
    c70 text,
    c71 text,
    c72 text,
    c73 text,
    c74 text,
    c75 text,
    c76 text,
    c77 text
) ON COMMIT DROP;

\copy stock_raw (c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15, c16, c17, c18, c19, c20, c21, c22, c23, c24, c25, c26, c27, c28, c29, c30, c31, c32, c33, c34, c35, c36, c37, c38, c39, c40, c41, c42, c43, c44, c45, c46, c47, c48, c49, c50, c51, c52, c53, c54, c55, c56, c57, c58, c59, c60, c61, c62, c63, c64, c65, c66, c67, c68, c69, c70, c71, c72, c73, c74, c75, c76, c77) FROM '/tmp/stock.csv' WITH (FORMAT csv, HEADER false, ENCODING 'UTF8')

-- ---------- 2. Check the header (first line) has the expected columns ----------
DO $$
DECLARE h stock_raw;
BEGIN
    SELECT * INTO h FROM stock_raw WHERE line_no = 1;
    IF btrim(replace(coalesce(h.c1, ''), chr(65279), '')) <> 'Stock #' THEN RAISE EXCEPTION 'Column 1 should be "Stock #" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c1; END IF;
    IF btrim(replace(coalesce(h.c2, ''), chr(65279), '')) <> 'Availability' THEN RAISE EXCEPTION 'Column 2 should be "Availability" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c2; END IF;
    IF btrim(replace(coalesce(h.c3, ''), chr(65279), '')) <> 'Shape' THEN RAISE EXCEPTION 'Column 3 should be "Shape" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c3; END IF;
    IF btrim(replace(coalesce(h.c4, ''), chr(65279), '')) <> 'Weight' THEN RAISE EXCEPTION 'Column 4 should be "Weight" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c4; END IF;
    IF btrim(replace(coalesce(h.c5, ''), chr(65279), '')) <> 'Clarity' THEN RAISE EXCEPTION 'Column 5 should be "Clarity" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c5; END IF;
    IF btrim(replace(coalesce(h.c6, ''), chr(65279), '')) <> 'Color' THEN RAISE EXCEPTION 'Column 6 should be "Color" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c6; END IF;
    IF btrim(replace(coalesce(h.c7, ''), chr(65279), '')) <> 'Fancy Color' THEN RAISE EXCEPTION 'Column 7 should be "Fancy Color" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c7; END IF;
    IF btrim(replace(coalesce(h.c10, ''), chr(65279), '')) <> 'Price' THEN RAISE EXCEPTION 'Column 10 should be "Price" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c10; END IF;
    IF btrim(replace(coalesce(h.c11, ''), chr(65279), '')) <> 'Total Price' THEN RAISE EXCEPTION 'Column 11 should be "Total Price" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c11; END IF;
    IF btrim(replace(coalesce(h.c13, ''), chr(65279), '')) <> 'Image Link' THEN RAISE EXCEPTION 'Column 13 should be "Image Link" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c13; END IF;
    IF btrim(replace(coalesce(h.c14, ''), chr(65279), '')) <> 'Image Link 2' THEN RAISE EXCEPTION 'Column 14 should be "Image Link 2" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c14; END IF;
    IF btrim(replace(coalesce(h.c15, ''), chr(65279), '')) <> 'Image Link 3' THEN RAISE EXCEPTION 'Column 15 should be "Image Link 3" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c15; END IF;
    IF btrim(replace(coalesce(h.c16, ''), chr(65279), '')) <> 'Image Link 4' THEN RAISE EXCEPTION 'Column 16 should be "Image Link 4" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c16; END IF;
    IF btrim(replace(coalesce(h.c17, ''), chr(65279), '')) <> 'Image Link 5' THEN RAISE EXCEPTION 'Column 17 should be "Image Link 5" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c17; END IF;
    IF btrim(replace(coalesce(h.c18, ''), chr(65279), '')) <> 'Image Link 6' THEN RAISE EXCEPTION 'Column 18 should be "Image Link 6" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c18; END IF;
    IF btrim(replace(coalesce(h.c19, ''), chr(65279), '')) <> 'Video Link' THEN RAISE EXCEPTION 'Column 19 should be "Video Link" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c19; END IF;
    IF btrim(replace(coalesce(h.c20, ''), chr(65279), '')) <> 'Cut Grade' THEN RAISE EXCEPTION 'Column 20 should be "Cut Grade" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c20; END IF;
    IF btrim(replace(coalesce(h.c21, ''), chr(65279), '')) <> 'Polish' THEN RAISE EXCEPTION 'Column 21 should be "Polish" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c21; END IF;
    IF btrim(replace(coalesce(h.c22, ''), chr(65279), '')) <> 'Symmetry' THEN RAISE EXCEPTION 'Column 22 should be "Symmetry" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c22; END IF;
    IF btrim(replace(coalesce(h.c23, ''), chr(65279), '')) <> 'Depth Percent' THEN RAISE EXCEPTION 'Column 23 should be "Depth Percent" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c23; END IF;
    IF btrim(replace(coalesce(h.c24, ''), chr(65279), '')) <> 'Table Percent' THEN RAISE EXCEPTION 'Column 24 should be "Table Percent" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c24; END IF;
    IF btrim(replace(coalesce(h.c25, ''), chr(65279), '')) <> 'Fluorescence Intensity' THEN RAISE EXCEPTION 'Column 25 should be "Fluorescence Intensity" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c25; END IF;
    IF btrim(replace(coalesce(h.c27, ''), chr(65279), '')) <> 'Lab' THEN RAISE EXCEPTION 'Column 27 should be "Lab" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c27; END IF;
    IF btrim(replace(coalesce(h.c28, ''), chr(65279), '')) <> 'Certificate #' THEN RAISE EXCEPTION 'Column 28 should be "Certificate #" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c28; END IF;
    IF btrim(replace(coalesce(h.c38, ''), chr(65279), '')) <> 'Measurements Depth' THEN RAISE EXCEPTION 'Column 38 should be "Measurements Depth" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c38; END IF;
    IF btrim(replace(coalesce(h.c39, ''), chr(65279), '')) <> 'Measurements Length' THEN RAISE EXCEPTION 'Column 39 should be "Measurements Length" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c39; END IF;
    IF btrim(replace(coalesce(h.c40, ''), chr(65279), '')) <> 'Measurements Width' THEN RAISE EXCEPTION 'Column 40 should be "Measurements Width" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c40; END IF;
    IF btrim(replace(coalesce(h.c62, ''), chr(65279), '')) <> 'Type' THEN RAISE EXCEPTION 'Column 62 should be "Type" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c62; END IF;
    IF btrim(replace(coalesce(h.c73, ''), chr(65279), '')) <> 'Short Title' THEN RAISE EXCEPTION 'Column 73 should be "Short Title" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c73; END IF;
    IF btrim(replace(coalesce(h.c77, ''), chr(65279), '')) <> 'Growth Type' THEN RAISE EXCEPTION 'Column 77 should be "Growth Type" but the file has "%". Save the original supplier sheet as CSV UTF-8 without moving columns.', h.c77; END IF;
END $$;

DELETE FROM stock_raw WHERE line_no = 1;
-- Ignore completely empty lines (Excel often adds some at the end)
DELETE FROM stock_raw WHERE btrim(coalesce(c1, '')) = '' AND btrim(coalesce(c3, '')) = '';

-- ---------- helpers ----------
-- Empty, "-" and spaces mean "no value"
CREATE FUNCTION pg_temp.clean(v text) RETURNS text LANGUAGE sql IMMUTABLE AS
$f$ SELECT NULLIF(NULLIF(btrim(v, E' \t\r\n '), ''), '-') $f$;
-- A number, or NULL if it isn't one
CREATE FUNCTION pg_temp.num(v text) RETURNS numeric LANGUAGE sql IMMUTABLE AS
$f$ SELECT CASE WHEN replace(pg_temp.clean(v), ',', '') ~ '^[0-9]+(\.[0-9]+)?$'
               THEN replace(pg_temp.clean(v), ',', '')::numeric END $f$;
CREATE FUNCTION pg_temp.finish(v text) RETURNS text LANGUAGE sql IMMUTABLE AS
$f$ SELECT CASE
     WHEN v IN ('ID','IDEAL','EX','EXC','EXCELLENT','X') THEN 'EXCELLENT'
     WHEN v IN ('VG','VERY GOOD','V.GOOD','V GOOD') THEN 'VERY_GOOD'
     WHEN v IN ('G','GD','GOOD') THEN 'GOOD'
     WHEN v IN ('F','FR','FAIR') THEN 'FAIR' END $f$;

-- ---------- 3. Read and translate every row ----------
CREATE TEMP TABLE stock ON COMMIT DROP AS
SELECT
    r.line_no                                                     AS excel_row,
    upper(pg_temp.clean(r.c1))                                    AS stock_no,
    upper(pg_temp.clean(r.c2))                                    AS availability,
    upper(pg_temp.clean(r.c3))                                    AS shape_raw,
    pg_temp.num(r.c4)                                             AS carat,
    upper(pg_temp.clean(r.c5))                                    AS clarity_raw,
    upper(pg_temp.clean(r.c6))                                    AS color_raw,
    pg_temp.clean(r.c7)                                           AS fancy_color,
    coalesce(pg_temp.num(r.c11), pg_temp.num(r.c10) * pg_temp.num(r.c4)) AS cost_usd,
    array_remove(ARRAY[pg_temp.clean(r.c13), pg_temp.clean(r.c14), pg_temp.clean(r.c15), pg_temp.clean(r.c16),
                       pg_temp.clean(r.c17), pg_temp.clean(r.c18), pg_temp.clean(r.c19)], NULL) AS links,
    upper(pg_temp.clean(r.c20))                                   AS cut_raw,
    upper(pg_temp.clean(r.c21))                                   AS polish_raw,
    upper(pg_temp.clean(r.c22))                                   AS symmetry_raw,
    pg_temp.num(r.c23)                                            AS depth_pct,
    pg_temp.num(r.c24)                                            AS table_pct,
    upper(pg_temp.clean(r.c25))                                   AS fluor_raw,
    upper(pg_temp.clean(r.c27))                                   AS lab_raw,
    upper(regexp_replace(coalesce(pg_temp.clean(r.c28), ''), '\s', '', 'g')) AS cert_raw,
    pg_temp.num(r.c38)                                            AS depth_mm,
    pg_temp.num(r.c39)                                            AS length_mm,
    pg_temp.num(r.c40)                                            AS width_mm,
    upper(pg_temp.clean(r.c62))                                   AS type_raw,
    pg_temp.clean(r.c73)                                          AS short_title,
    upper(pg_temp.clean(r.c77))                                   AS growth_raw
FROM stock_raw r;

ALTER TABLE stock
    ADD COLUMN shape text, ADD COLUMN color text, ADD COLUMN clarity text, ADD COLUMN cut text,
    ADD COLUMN cut_invalid boolean, ADD COLUMN polish text, ADD COLUMN symmetry text,
    ADD COLUMN fluorescence text, ADD COLUMN lab text, ADD COLUMN origin text, ADD COLUMN growth_type text,
    ADD COLUMN certificate_number text, ADD COLUMN price_paise bigint,
    ADD COLUMN images text[], ADD COLUMN video_url text, ADD COLUMN media_mismatch boolean,
    ADD COLUMN errors text[], ADD COLUMN warnings text[];

UPDATE stock SET
    shape = CASE
        WHEN shape_raw IN ('ROUND','RD','BR','RBC','ROUND BRILLIANT') THEN 'ROUND'
        WHEN shape_raw IN ('OVAL','OV','OVAL BRILLIANT') THEN 'OVAL'
        WHEN shape_raw IN ('PRINCESS','PR','PRN','PRINCESS CUT') THEN 'PRINCESS'
        WHEN shape_raw LIKE 'CUSHION%' OR shape_raw IN ('CU','CUS','CB','CMB') THEN 'CUSHION'
        WHEN shape_raw IN ('EMERALD','EM','EMR','EMERALD CUT') THEN 'EMERALD'
        WHEN shape_raw IN ('ASSCHER','AS','ASC','SQ EMERALD','SQUARE EMERALD','SQEM','SE') THEN 'ASSCHER'
        WHEN shape_raw IN ('PEAR','PS','PE','PEAR SHAPE') THEN 'PEAR'
        WHEN shape_raw IN ('MARQUISE','MQ','MAR') THEN 'MARQUISE'
        WHEN shape_raw IN ('HEART','HS','HT','HRT','HEART SHAPE') THEN 'HEART'
        WHEN shape_raw LIKE '%RADIANT%' OR shape_raw IN ('RAD','RA') THEN 'RADIANT'
    END,
    color   = CASE WHEN fancy_color IS NULL AND color_raw IN ('D','E','F','G','H','I','J','K','L','M') THEN color_raw END,
    clarity = CASE WHEN clarity_raw IN ('FL','IF','VVS1','VVS2','VS1','VS2','SI1','SI2','I1','I2','I3') THEN clarity_raw END,
    cut = CASE
        WHEN cut_raw IN ('ID','IDEAL','I') THEN 'IDEAL'
        WHEN cut_raw IN ('EX','EXC','EXCELLENT','X') THEN 'EXCELLENT'
        WHEN cut_raw IN ('VG','VERY GOOD','V.GOOD','V GOOD') THEN 'VERY_GOOD'
        WHEN cut_raw IN ('G','GD','GOOD') THEN 'GOOD'
        WHEN cut_raw IN ('F','FR','FAIR') THEN 'FAIR' END,
    cut_invalid = cut_raw IS NOT NULL AND cut_raw NOT IN ('N/A','NA','NONE','ID','IDEAL','I','EX','EXC','EXCELLENT','X',
                  'VG','VERY GOOD','V.GOOD','V GOOD','G','GD','GOOD','F','FR','FAIR'),
    polish   = pg_temp.finish(polish_raw),
    symmetry = pg_temp.finish(symmetry_raw),
    fluorescence = CASE
        WHEN fluor_raw IS NULL OR fluor_raw IN ('N','NON','NONE','NIL','NO') THEN 'NONE'
        WHEN fluor_raw IN ('F','FNT','FAINT','SL','SLT','SLIGHT','VSL','VSLT','VERY SLIGHT') THEN 'FAINT'
        WHEN fluor_raw IN ('M','MED','MEDIUM') THEN 'MEDIUM'
        WHEN fluor_raw IN ('S','ST','STG','STRONG') THEN 'STRONG'
        WHEN fluor_raw IN ('VS','VST','VSTG','VERY STRONG') THEN 'VERY_STRONG' END,
    lab = CASE WHEN lab_raw IN ('GIA','IGI','HRD') THEN lab_raw END,
    origin = CASE WHEN growth_raw IN ('HPHT','CVD') OR type_raw IN ('HPHT','CVD')
                       OR short_title ILIKE '%lab%grown%' OR short_title ILIKE '%lab-grown%'
                  THEN 'LAB_GROWN' ELSE 'NATURAL' END,
    growth_type = CASE WHEN growth_raw IN ('HPHT','CVD') THEN growth_raw
                       WHEN type_raw IN ('HPHT','CVD') THEN type_raw END,
    certificate_number = CASE WHEN cert_raw ~ '^[A-Z0-9]{5,20}$' THEN cert_raw END,
    price_paise = (round(cost_usd * :'rate'::numeric * :'markup'::numeric / :'round_to'::numeric)
                   * :'round_to'::numeric * 100)::bigint,
    -- Photos = links ending in .jpg/.png/.webp; the 360 viewer = gem360 page or video file
    images = ARRAY(SELECT l FROM unnest(links) WITH ORDINALITY AS t(l, n)
                   WHERE l ~* '\.(jpe?g|png|webp)(\?.*)?$' ORDER BY n),
    video_url = (SELECT l FROM unnest(links) WITH ORDINALITY AS t(l, n)
                 WHERE l !~* '\.(jpe?g|png|webp)(\?.*)?$' AND l ~* '^https://' ORDER BY n LIMIT 1),
    -- Every link should contain this stone's Stock #; otherwise it shows another stone
    media_mismatch = EXISTS (SELECT 1 FROM unnest(links) l WHERE position(stock_no IN upper(l)) = 0);

-- Problems that stop a row from being imported
UPDATE stock s SET errors = array_remove(ARRAY[
    CASE WHEN s.stock_no IS NULL THEN 'Stock # is empty'
         WHEN s.stock_no !~ '^[A-Z0-9][A-Z0-9_-]{1,59}$' THEN 'Stock # may only use letters, numbers, - and _' END,
    CASE WHEN (SELECT count(*) FROM stock d WHERE d.stock_no = s.stock_no) > 1 THEN 'Stock # appears more than once in the file' END,
    CASE WHEN s.shape IS NULL THEN 'unknown shape "' || coalesce(s.shape_raw, '') || '"' END,
    CASE WHEN s.carat IS NULL OR s.carat <= 0 OR s.carat > 50 THEN 'invalid Weight' END,
    CASE WHEN s.fancy_color IS NOT NULL THEN 'fancy colour diamonds are not supported yet'
         WHEN s.color IS NULL THEN 'unknown Color "' || coalesce(s.color_raw, '') || '"' END,
    CASE WHEN s.clarity IS NULL THEN 'unknown Clarity "' || coalesce(s.clarity_raw, '') || '"' END,
    CASE WHEN s.cut_invalid THEN 'unknown Cut Grade "' || s.cut_raw || '"' END,
    CASE WHEN s.polish IS NULL THEN 'unknown or empty Polish "' || coalesce(s.polish_raw, '') || '"' END,
    CASE WHEN s.symmetry IS NULL THEN 'unknown or empty Symmetry "' || coalesce(s.symmetry_raw, '') || '"' END,
    CASE WHEN s.fluorescence IS NULL THEN 'unknown Fluorescence "' || s.fluor_raw || '"' END,
    CASE WHEN s.lab IS NULL THEN 'Lab must be GIA, IGI or HRD (got "' || coalesce(s.lab_raw, '') || '")' END,
    CASE WHEN s.price_paise IS NULL OR s.price_paise < 10000 THEN 'missing or invalid Total Price / Price' END,
    CASE WHEN s.certificate_number IS NOT NULL
              AND (SELECT count(*) FROM stock d WHERE d.certificate_number = s.certificate_number AND d.lab = s.lab) > 1
         THEN 'Certificate # ' || s.certificate_number || ' appears more than once in the file' END,
    CASE WHEN s.certificate_number IS NOT NULL AND EXISTS (
              SELECT 1 FROM diamond_details dd JOIN products p ON p.id = dd.product_id
              WHERE dd.certificate_number = s.certificate_number AND dd.lab = s.lab AND p.slug <> lower(s.stock_no))
         THEN 'Certificate # ' || s.certificate_number || ' already belongs to another diamond in the shop' END
], NULL);

-- Problems that are reported but don't stop the import
UPDATE stock s SET warnings = array_remove(ARRAY[
    CASE WHEN s.media_mismatch THEN 'photo/video links belong to another Stock # - imported WITHOUT photos/video' END,
    CASE WHEN NOT s.media_mismatch AND cardinality(s.images) = 0 THEN 'no photo' END,
    CASE WHEN s.availability IS DISTINCT FROM 'YES' THEN 'Availability is not YES - hidden from the shop' END,
    CASE WHEN s.cert_raw <> '' AND s.certificate_number IS NULL THEN 'Certificate # "' || s.cert_raw || '" looks invalid - left empty' END
], NULL);

UPDATE stock SET images = '{}', video_url = NULL WHERE media_mismatch;

-- ---------- 4. Report ----------
\echo
\echo '==================== ROWS NOT IMPORTED (fix them in Excel) ===================='
SELECT excel_row AS "Excel row", stock_no AS "Stock #", array_to_string(errors, '; ') AS "Problem"
FROM stock WHERE cardinality(errors) > 0 ORDER BY excel_row;

\echo '==================== WARNINGS (imported, but please check) ===================='
SELECT excel_row AS "Excel row", stock_no AS "Stock #", array_to_string(warnings, '; ') AS "Warning"
FROM stock WHERE cardinality(errors) = 0 AND cardinality(warnings) > 0 ORDER BY excel_row;

-- ---------- 5. Save the good rows ----------
CREATE TEMP TABLE good ON COMMIT DROP AS
SELECT s.*,
       lower(s.stock_no) AS slug,
       EXISTS (SELECT 1 FROM products p WHERE p.slug = lower(s.stock_no)) AS already_in_shop,
       to_char(s.carat, 'FM990.00') || ' ct ' || s.color || ' ' || s.clarity || ' ' || initcap(s.shape) || ' '
           || CASE WHEN s.origin = 'LAB_GROWN' THEN 'Lab-grown' ELSE 'Natural' END || ' Diamond' AS name,
       to_char(s.carat, 'FM990.00') || ' carat ' || lower(s.shape) || ' '
           || CASE WHEN s.origin = 'LAB_GROWN' THEN 'lab-grown' ELSE 'natural' END || ' diamond'
           || coalesce(' (' || s.growth_type || ')', '')
           || ', ' || s.color || ' colour, ' || s.clarity || ' clarity'
           || coalesce(', ' || initcap(replace(s.cut, '_', ' ')) || ' cut', '')
           || ', ' || s.lab || ' certified'
           || coalesce(' (report ' || s.certificate_number || ')', '') || '.' AS description
FROM stock s
WHERE cardinality(s.errors) = 0;

INSERT INTO products (id, slug, name, description, status)
SELECT gen_random_uuid(), g.slug, g.name, g.description,
       CASE WHEN g.availability = 'YES' THEN 'ACTIVE' ELSE 'ARCHIVED' END
FROM good g
ON CONFLICT (slug) DO UPDATE
    SET name = EXCLUDED.name, description = EXCLUDED.description, status = EXCLUDED.status;

-- New stones get stock 1. Existing stones keep their stock, so a stone sold or reserved
-- on our website is never put back on sale by a re-import.
INSERT INTO product_variants (id, product_id, sku, name, price_paise, mrp_paise, stock_quantity, active, sort_order)
SELECT gen_random_uuid(), p.id, g.stock_no, 'Single stone', g.price_paise, NULL, 1, true, 0
FROM good g JOIN products p ON p.slug = g.slug
ON CONFLICT (sku) DO UPDATE SET price_paise = EXCLUDED.price_paise, active = true;

INSERT INTO diamond_details (product_id, origin, shape, carat, color, clarity, cut, polish, symmetry,
                             fluorescence, lab, quick_ship, certificate_number, growth_type,
                             length_mm, width_mm, depth_mm, table_pct, depth_pct, video_url)
SELECT p.id, g.origin, g.shape, g.carat, g.color, g.clarity, g.cut, g.polish, g.symmetry,
       g.fluorescence, g.lab, false, g.certificate_number, g.growth_type,
       CASE WHEN g.length_mm > 0 AND g.length_mm < 100 THEN g.length_mm END,
       CASE WHEN g.width_mm  > 0 AND g.width_mm  < 100 THEN g.width_mm  END,
       CASE WHEN g.depth_mm  > 0 AND g.depth_mm  < 100 THEN g.depth_mm  END,
       CASE WHEN g.table_pct > 0 AND g.table_pct < 100 THEN g.table_pct END,
       CASE WHEN g.depth_pct > 0 AND g.depth_pct < 100 THEN g.depth_pct END,
       g.video_url
FROM good g JOIN products p ON p.slug = g.slug
ON CONFLICT (product_id) DO UPDATE SET
    origin = EXCLUDED.origin, shape = EXCLUDED.shape, carat = EXCLUDED.carat, color = EXCLUDED.color,
    clarity = EXCLUDED.clarity, cut = EXCLUDED.cut, polish = EXCLUDED.polish, symmetry = EXCLUDED.symmetry,
    fluorescence = EXCLUDED.fluorescence, lab = EXCLUDED.lab, certificate_number = EXCLUDED.certificate_number,
    growth_type = EXCLUDED.growth_type, length_mm = EXCLUDED.length_mm, width_mm = EXCLUDED.width_mm,
    depth_mm = EXCLUDED.depth_mm, table_pct = EXCLUDED.table_pct, depth_pct = EXCLUDED.depth_pct,
    video_url = EXCLUDED.video_url;

-- Photos: replaced with the ones from the file
DELETE FROM product_images WHERE product_id IN (SELECT p.id FROM good g JOIN products p ON p.slug = g.slug);
INSERT INTO product_images (id, product_id, url, alt_text, sort_order)
SELECT gen_random_uuid(), p.id, img.url, g.name, (img.n - 1)::int
FROM good g
JOIN products p ON p.slug = g.slug
CROSS JOIN LATERAL unnest(g.images) WITH ORDINALITY AS img(url, n);

-- ---------- 6. Summary ----------
\echo '==================== SUMMARY ===================='
SELECT
    (SELECT count(*) FROM stock)                                                   AS "Rows in file",
    (SELECT count(*) FROM good WHERE NOT already_in_shop)                          AS "New diamonds",
    (SELECT count(*) FROM good WHERE already_in_shop)                              AS "Updated diamonds",
    (SELECT count(*) FROM good WHERE availability = 'YES')                         AS "On sale",
    (SELECT count(*) FROM good WHERE availability IS DISTINCT FROM 'YES')          AS "Hidden (not YES)",
    (SELECT count(*) FROM stock WHERE cardinality(errors) > 0)                     AS "Not imported";

\echo 'Price check (first 5 on sale):'
SELECT stock_no AS "Stock #", carat AS "ct", cost_usd AS "Cost USD",
       to_char(price_paise / 100, 'FM99,99,99,999') AS "Selling price INR"
FROM good WHERE availability = 'YES' ORDER BY excel_row LIMIT 5;

\if :{?apply}
COMMIT;
\echo '>>> SAVED. The diamonds are now in the shop.'
\else
ROLLBACK;
\echo '>>> PREVIEW ONLY - nothing was saved. Run again with  -v apply=1  to save.'
\endif
