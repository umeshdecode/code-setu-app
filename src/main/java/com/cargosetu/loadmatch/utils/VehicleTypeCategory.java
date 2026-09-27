package com.cargosetu.loadmatch.utils;

/**
 * Canonical vehicle-type taxonomy used across Vehicle/Shipment/Trip validation.
 * Kept as plain String constants (not an enum) because vehicleType /
 * vehicleTypeRequired / vehicleTypeCategory are plain String fields on the
 * entities/DTOs; switching those to an enum type would be a larger, riskier
 * schema/type change than this validation-only task calls for.
 */
public final class VehicleTypeCategory {

    public static final String OPEN = "OPEN";
    public static final String CONTAINER = "CONTAINER";
    public static final String LCV = "LCV";
    public static final String MINI_PICKUP = "MINI_PICKUP";
    public static final String TRAILER = "TRAILER";
    public static final String TIPPER = "TIPPER";
    public static final String TANKER = "TANKER";
    public static final String DUMPER = "DUMPER";
    public static final String BULKER = "BULKER";

    public static final String VALIDATION_PATTERN =
            "OPEN|CONTAINER|LCV|MINI_PICKUP|TRAILER|TIPPER|TANKER|DUMPER|BULKER";

    private VehicleTypeCategory() {
    }
}
