package com.cargosetu.marketplace.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CapacityUnit {
    KG, TONNE, POUND, CBM, SQ_FT;

    @JsonCreator
    public static CapacityUnit fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return TONNE;
        }
        String upper = value.trim().toUpperCase();
        switch (upper) {
            case "TONS":
            case "TON":
            case "TONNE":
            case "TONNES":
            case "MT":
                return TONNE;
            case "KG":
            case "KGS":
            case "KILOGRAM":
            case "KILOGRAMS":
                return KG;
            case "POUND":
            case "POUNDS":
            case "LBS":
                return POUND;
            case "CBM":
                return CBM;
            case "SQ_FT":
            case "SQFT":
                return SQ_FT;
            default:
                try {
                    return CapacityUnit.valueOf(upper);
                } catch (IllegalArgumentException e) {
                    return TONNE;
                }
        }
    }
}
