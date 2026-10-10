package com.cargosetu.marketplace.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum PaymentTermType {
    ADVANCE_PERCENT("Advance Percent"),
    HALF_ADVANCE("Half Advance"),
    FULL_ADVANCE("Full Advance"),
    TO_PAY("To Pay"),
    PARTIAL_TO_PAY("Partial To Pay"),
    CREDIT("Credit"),
    CASH_TO_DRIVER("Cash To Driver"),
    DIRECT_TO_OWNER("Direct To Owner");

    private final String displayName;

    PaymentTermType(String displayName) {
        this.displayName = displayName;
    }

    @JsonCreator
    public static PaymentTermType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ADVANCE_PERCENT;
        }
        String upper = value.trim().toUpperCase();
        if (upper.equals("ADVANCE_BALANCE")) {
            return ADVANCE_PERCENT;
        }
        for (PaymentTermType pt : values()) {
            if (pt.name().equalsIgnoreCase(upper) || pt.displayName.equalsIgnoreCase(value.trim())) {
                return pt;
            }
        }
        return ADVANCE_PERCENT;
    }
}
