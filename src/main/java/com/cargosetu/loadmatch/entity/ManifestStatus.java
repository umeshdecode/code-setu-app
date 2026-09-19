package com.cargosetu.loadmatch.entity;

/**
 * Lifecycle of a TripShipment manifest row, i.e. a shipment that has actually
 * been allocated onto a trip (as opposed to a LoadMatch, which is only a
 * suggestion/negotiation record). New rows created by acceptMatch always
 * start at CONFIRMED.
 */
public enum ManifestStatus {
    CONFIRMED,
    LOADED,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED
}
