package com.cargosetu.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class TripDocument {

    @Id
    private Long id;

    @ManyToOne
    private Trip trip;

    private String documentType;

    private String documentUrl;
}