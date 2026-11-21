package com.cargosetu.marketplace.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum CargoType {
    ALL("All"),
    CARTON("Carton"),
    FRAGILE("Fragile"),
    FOOD("Food"),
    ELECTRONICS("Electronics"),
    TEXTILE("Textile"),
    INDUSTRIAL("Industrial"),
    PERISHABLE("Perishable"),
    HAZARDOUS("Hazardous"),
    FMCG("FMCG"),
    GENERAL("General");

    private final String displayName;

    CargoType(String displayName) {
        this.displayName = displayName;
    }

    @JsonCreator
    public static CargoType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return GENERAL;
        }
        String upper = value.trim().toUpperCase();
        for (CargoType ct : values()) {
            if (ct.name().equalsIgnoreCase(upper) || ct.displayName.equalsIgnoreCase(value.trim())) {
                return ct;
            }
        }
        return GENERAL;
    }
}
