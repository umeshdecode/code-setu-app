package com.cargosetu.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
@Entity
@Table(name = "tblmvahicletrack")
@Data
public class VehicleLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long vehicleId;

    private double latitude;
    private double longitude;

    private LocalDateTime timestamp;
}
