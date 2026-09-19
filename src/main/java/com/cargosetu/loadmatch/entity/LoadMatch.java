package com.cargosetu.loadmatch.entity;

import com.cargosetu.abstracts.AbstractModel;
import com.cargosetu.model.Trip;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "tblm_load_match")
@Data
public class LoadMatch extends AbstractModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long loadMatchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    private Double overallScore;

    private Double routeScore;

    private Double capacityScore;

    private Double weightScore;

    private Double timingScore;

    private Double vehicleScore;

    private Double handlingScore;

    @Enumerated(EnumType.STRING)
    private LoadMatchStatus status;

    private LocalDateTime expiresAt;

    private Integer companyId;

    // Phase 3: free-text reason captured when a match is rejected. Nullable -
    // populated only by rejectMatch.
    private String rejectionReason;
}
