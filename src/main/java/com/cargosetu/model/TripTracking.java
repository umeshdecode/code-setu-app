package com.cargosetu.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;


@Entity
public class TripTracking {

    @Id
    private Long id;

    @ManyToOne
    private Trip trip;

    private Double latitude;

    private Double longitude;

    private String location;

    private LocalDateTime trackedAt;
}