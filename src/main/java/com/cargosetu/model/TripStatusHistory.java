package com.cargosetu.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

@Entity
public class TripStatusHistory {

    @Id
    private Long id;

    @ManyToOne
    private Trip trip;

    private String oldStatus;

    private String newStatus;

    private String changedBy;

    private LocalDateTime changedAt;
}