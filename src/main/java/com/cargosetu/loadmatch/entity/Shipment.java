package com.cargosetu.loadmatch.entity;

import com.cargosetu.abstracts.AbstractModel;
import com.cargosetu.customer.entity.Customer;
import com.cargosetu.loadmatch.utils.VehicleTypeCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
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

    // Optional: empty/null means "any vehicle type OK" (see MatchScoringEngine.scoreVehicle).
    // @Pattern does not fire on null, so null stays valid; "^$|" also allows empty string.
    @Pattern(regexp = "^$|" + VehicleTypeCategory.VALIDATION_PATTERN, message = "Invalid vehicle type")
    private String vehicleTypeRequired;

    @Enumerated(EnumType.STRING)
    private ShipmentStatus status;

    private Integer companyId;

    private Integer custId;
}
