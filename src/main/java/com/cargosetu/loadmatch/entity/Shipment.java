package com.cargosetu.loadmatch.entity;

import com.cargosetu.abstracts.AbstractModel;
import com.cargosetu.customer.entity.Customer;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "tblm_shipment")
@Data
public class Shipment extends AbstractModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long shipmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    private String sourceCity;

    private String destinationCity;

    private String pickupAddress;

    private String dropAddress;

    private String cargoType;

    private Double totalWeightKg;

    private Double totalVolumeCbm;

    private Double lengthCm;

    private Double widthCm;

    private Double heightCm;

    private LocalDate requiredPickupDate;

    private LocalDate requiredDeliveryDate;

    // comma-separated tags, e.g. "FRAGILE,TEMPERATURE_CONTROLLED"
    private String specialHandling;

    private String vehicleTypeRequired;

    @Enumerated(EnumType.STRING)
    private ShipmentStatus status;

    private Integer companyId;

    private Integer custId;
}
