package com.cargosetu.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "trip_status_history")
@Data
public class TripStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Trip trip;

    private String oldStatus;

    private String newStatus;

    private String changedBy;

    private LocalDateTime changedAt;
}