package com.cargosetu.loadmatch.service.matching;

/**
 * Weighting of each sub-score toward the overall match score. Kept as plain
 * constants (not a Spring bean) since these are compile-time-fixed and don't
 * need injection anywhere.
 */
public final class MatchWeights {

    private MatchWeights() {
    }

    public static final double ROUTE = 0.35;
    public static final double CAPACITY = 0.25;
    public static final double WEIGHT = 0.15;
    public static final double TIMING = 0.10;
    public static final double VEHICLE = 0.10;
    public static final double HANDLING = 0.05;
}
