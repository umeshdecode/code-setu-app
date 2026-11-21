package com.cargosetu.marketplace.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TruckAvailabilityStatus {
    EMPTY_STANDING, EMPTY_MOVING, PARTIALLY_LOADED, FULL, BOOKED, UNAVAILABLE;

    @JsonCreator
    public static TruckAvailabilityStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return EMPTY_STANDING;
        }
        String upper = value.trim().toUpperCase();
        switch (upper) {
            case "AVAILABLE":
            case "EMPTY":
                return EMPTY_STANDING;
            default:
                try {
                    return TruckAvailabilityStatus.valueOf(upper);
                } catch (IllegalArgumentException e) {
                    return EMPTY_STANDING;
                }
        }
    }
}
