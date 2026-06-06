package com.cargosetu.customer.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tblt_shift_package")
public class ShiftPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long packageId;

    private String packageNumber;

    private Boolean loaded = false;

    private Boolean unloaded = false;

    private LocalDateTime loadedAt;

    private LocalDateTime unloadedAt;

    @ManyToOne
    @JoinColumn(name = "shifting_id")
    private Shifting shifting;
}