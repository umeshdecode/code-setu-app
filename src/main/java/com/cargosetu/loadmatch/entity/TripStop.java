package com.cargosetu.loadmatch.entity;

import com.cargosetu.abstracts.AbstractModel;
import com.cargosetu.model.Trip;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tblm_trip_stop")
@Data
public class TripStop extends AbstractModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tripStopId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    private String stopCity;

    private Integer sequenceOrder;
}
