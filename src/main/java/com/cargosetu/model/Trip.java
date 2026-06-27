package com.cargosetu.model;

import com.cargosetu.customer.entity.Shifting;
import jakarta.persistence.*;
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
}