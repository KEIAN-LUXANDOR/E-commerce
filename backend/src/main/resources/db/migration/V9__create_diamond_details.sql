-- V9: one row of grading details per diamond product, used by the shop filters
CREATE TABLE diamond_details (
    product_id    UUID          PRIMARY KEY REFERENCES products(id) ON DELETE CASCADE,
    origin        VARCHAR(10)   NOT NULL CHECK (origin IN ('NATURAL','LAB_GROWN')),
    shape         VARCHAR(10)   NOT NULL CHECK (shape IN ('ROUND','CUSHION','EMERALD','PEAR','ASSCHER',
                                                          'PRINCESS','OVAL','HEART','MARQUISE','RADIANT')),
    carat         NUMERIC(5,2)  NOT NULL CHECK (carat > 0 AND carat <= 50),
    color         VARCHAR(1)    NOT NULL CHECK (color IN ('D','E','F','G','H','I','J','K','L','M')),
    clarity       VARCHAR(4)    NOT NULL CHECK (clarity IN ('FL','IF','VVS1','VVS2','VS1','VS2',
                                                            'SI1','SI2','I1','I2','I3')),
    cut           VARCHAR(10)   CHECK (cut IN ('IDEAL','EXCELLENT','VERY_GOOD','GOOD','FAIR')),
    polish        VARCHAR(10)   NOT NULL CHECK (polish IN ('EXCELLENT','VERY_GOOD','GOOD','FAIR')),
    symmetry      VARCHAR(10)   NOT NULL CHECK (symmetry IN ('EXCELLENT','VERY_GOOD','GOOD','FAIR')),
    fluorescence  VARCHAR(12)   NOT NULL CHECK (fluorescence IN ('NONE','FAINT','MEDIUM','STRONG','VERY_STRONG')),
    lab           VARCHAR(5)    NOT NULL CHECK (lab IN ('GIA','IGI','HRD')),
    quick_ship    BOOLEAN       NOT NULL DEFAULT false
);

CREATE INDEX diamond_details_origin_shape_carat_idx ON diamond_details (origin, shape, carat);