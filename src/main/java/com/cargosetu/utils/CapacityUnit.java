package com.cargosetu.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Supported capacity units and conversions for CargoSetu.
 * Canonical storage unit is KG.
 */
public enum CapacityUnit {
    KG(1.0),
    POUND(0.453592),
    TONNE(1000.0);

    private final double toKgFactor;

    CapacityUnit(double toKgFactor) {
        this.toKgFactor = toKgFactor;
    }

    public double getToKgFactor() {
        return toKgFactor;
    }

    /**
     * Converts a given value in this unit to canonical kilograms.
     */
    public double toKg(double value) {
        return value * toKgFactor;
    }

    /**
     * Converts canonical kilograms to this unit.
     */
    public double fromKg(double kgValue) {
        return kgValue / toKgFactor;
    }

    /**
     * Converts between any two capacity units.
     */
    public static double convert(double value, CapacityUnit from, CapacityUnit to) {
        if (from == to) {
            return value;
        }
        double inKg = from.toKg(value);
        return to.fromKg(inKg);
    }

    /**
     * Parses a string unit name safely, defaulting to KG if null or unknown.
     */
    public static CapacityUnit fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return KG;
        }
        String cleaned = text.trim().toUpperCase();
        if (cleaned.startsWith("TON") || cleaned.equals("T")) {
            return TONNE;
        }
        if (cleaned.startsWith("LB") || cleaned.startsWith("POUND")) {
            return POUND;
        }
        return KG;
    }
}
