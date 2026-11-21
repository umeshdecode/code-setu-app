package com.cargosetu.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "trip_document")
@Data
public class TripDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Trip trip;

    private String documentType;

    private String documentUrl;

    private String fileName;
}