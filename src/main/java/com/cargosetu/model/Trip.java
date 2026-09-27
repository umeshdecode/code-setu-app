package com.cargosetu.model;

import com.cargosetu.customer.entity.Shifting;
import com.cargosetu.loadmatch.utils.VehicleTypeCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_trip")
@Data
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tripId;

    private String tripNumber;

    @ManyToOne
    private Shifting shifting;

    @ManyToOne
    private Driver driver;

    @ManyToOne
    private Vehicle vehicle;

    private LocalDateTime plannedStartTime;
    private LocalDateTime plannedEenTime;

    private LocalDateTime actualStartTime;

    private LocalDateTime actualEndTime;

    private Double plannedDistanceKm;

    private Double actualDistanceKm;

    @Enumerated(EnumType.STRING)
    private TripStatus status;

    private String remarks;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
    private Integer companyId;
    private Integer custId;

    // Smart Load Matching - capacity tracking fields.
    // Phase 0 baseline decision: this app's existing Shifting/ShiftInventoryItem
    // records carry no weight/volume, so remaining capacity is NEVER auto-derived
    // by silently deducting a guessed value for cargo already on the trip.
    // totalWeightKg/totalVolumeCbm are seeded from the assigned Vehicle's capacity
    // (via TripCapacityService.initializeCapacity), and staff MUST explicitly set
    // remainingWeightKg/remainingVolumeCbm to reflect whatever share of that total
    // is already consumed by cargo the trip is carrying today (PUT /trip/{id}/capacity)
    // before the trip is eligible for matching. Every subsequent decrement beyond
    // that staff-declared baseline comes only from accepted LoadMatch/TripShipment rows.
    private Double remainingWeightKg;

    private Double remainingVolumeCbm;

    private Double totalWeightKg;

    private Double totalVolumeCbm;

    private Double vehicleCapacityKg;

    private Double vehicleCapacityCbm;

    // Optional: a trip's capacity may not have this set yet. @Pattern doesn't
    // fire on null, and "^$|" also allows an empty string through.
    @Pattern(regexp = "^$|" + VehicleTypeCategory.VALIDATION_PATTERN, message = "Invalid vehicle type category")
    private String vehicleTypeCategory;

    private String equipmentTags;

    // Smart Load Matching Phase 3: optimistic-locking guard for concurrent
    // capacity mutations (acceptMatch / manifest cancellation release). No
    // other entity in this codebase uses @Version, so Long is chosen as the
    // conventional/simplest JPA optimistic-lock counter type. Backed by
    // tbl_trip.version (NOT NULL DEFAULT 0 - Hibernate requires a non-null
    // version value once managed).
    @Version
    private Long version;
}