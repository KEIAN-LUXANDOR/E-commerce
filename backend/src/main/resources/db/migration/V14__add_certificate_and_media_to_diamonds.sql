-- V14: certificate, measurements, growth type and 360 video for each diamond (all optional)
ALTER TABLE diamond_details
    ADD COLUMN certificate_number VARCHAR(20)   CHECK (certificate_number ~ '^[A-Z0-9]{5,20}$'),
    ADD COLUMN growth_type        VARCHAR(10)   CHECK (growth_type IN ('HPHT','CVD')),
    ADD COLUMN length_mm          NUMERIC(5,2)  CHECK (length_mm > 0 AND length_mm < 100),
    ADD COLUMN width_mm           NUMERIC(5,2)  CHECK (width_mm > 0 AND width_mm < 100),
    ADD COLUMN depth_mm           NUMERIC(5,2)  CHECK (depth_mm > 0 AND depth_mm < 100),
    ADD COLUMN table_pct          NUMERIC(4,1)  CHECK (table_pct > 0 AND table_pct < 100),
    ADD COLUMN depth_pct          NUMERIC(4,1)  CHECK (depth_pct > 0 AND depth_pct < 100),
    ADD COLUMN video_url          VARCHAR(1000);

-- The same report number can't be used twice by the same lab
CREATE UNIQUE INDEX diamond_details_lab_certificate_uk
    ON diamond_details (lab, certificate_number)
    WHERE certificate_number IS NOT NULL;