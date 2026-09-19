package com.cargosetu.loadmatch.entity;

import com.cargosetu.abstracts.AbstractModel;
import com.cargosetu.model.Trip;
import jakarta.persistence.*;
import lombok.Data;

/**
 * A confirmed manifest entry: a Shipment actually allocated onto a Trip,
 * created by LoadMatchingService.acceptMatch. loadMatch is nullable because a
 * manifest entry could in theory be created without a formal LoadMatch (e.g.
 * a manual override) - though this phase only ever populates it via
 * acceptMatch.
 */
@Entity
@Table(name = "tblm_trip_shipment")
@Data
public class TripShipment extends AbstractModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tripShipmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "load_match_id", nullable = true)
    private LoadMatch loadMatch;

    private Double allocatedWeightKg;

    private Double allocatedVolumeCbm;

    // Delivery order on the trip's route. Not computed automatically this
    // phase - a settable field only.
    private Integer sequenceOrder;

    @Enumerated(EnumType.STRING)
    private ManifestStatus manifestStatus;

    private Integer companyId;
}
