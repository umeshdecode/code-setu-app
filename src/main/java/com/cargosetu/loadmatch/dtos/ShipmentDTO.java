package com.cargosetu.loadmatch.dtos;

import com.cargosetu.loadmatch.entity.ShipmentStatus;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ShipmentDTO {
    private Long shipmentId;
    private Long customerId;
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
    private String specialHandling;
    private String vehicleTypeRequired;
    private ShipmentStatus status;
    private Integer companyId;
    private Integer custId;

    private List<ShipmentItemDTO> items;
}
