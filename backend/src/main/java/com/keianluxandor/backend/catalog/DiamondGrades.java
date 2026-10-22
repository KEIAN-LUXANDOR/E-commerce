package com.keianluxandor.backend.catalog;

// All grade values a diamond can have. These must match the CHECK lists in V9.
public class DiamondGrades {

    private DiamondGrades() {
    }

    public enum Origin { NATURAL, LAB_GROWN }

    public enum Shape { ROUND, CUSHION, EMERALD, PEAR, ASSCHER, PRINCESS, OVAL, HEART, MARQUISE, RADIANT }

    public enum Color { D, E, F, G, H, I, J, K, L, M }

    public enum Clarity { FL, IF, VVS1, VVS2, VS1, VS2, SI1, SI2, I1, I2, I3 }

    public enum Cut { IDEAL, EXCELLENT, VERY_GOOD, GOOD, FAIR }

    // Used for both polish and symmetry
    public enum Finish { EXCELLENT, VERY_GOOD, GOOD, FAIR }

    public enum Fluorescence { NONE, FAINT, MEDIUM, STRONG, VERY_STRONG }

    public enum Lab { GIA, IGI, HRD }
}