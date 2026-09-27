package com.cargosetu.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "vehicle_manufacturer_models")
public class VehicleManufacturerModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String manufacturer;

    @Column(nullable = false)
    private String model;

    // Loosely maps to VehicleTypeCategory.VALIDATION_PATTERN values; nullable since
    // some models legitimately span more than one category (e.g. tipper/tanker variants).
    private String vehicleTypeCategory;
}
