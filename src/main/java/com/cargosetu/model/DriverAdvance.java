package com.cargosetu.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

@Entity
public class DriverAdvance {

    @Id
    private Long id;

    @ManyToOne
    private Trip trip;

    @ManyToOne
    private Driver driver;

    private Double amount;

    private String paymentMode;

    private LocalDateTime paidAt;
}