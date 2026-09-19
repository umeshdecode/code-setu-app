package com.cargosetu.loadmatch.dtos;

import lombok.Data;

@Data
public class ShipmentItemDTO {
    private Long shipmentItemId;
    private Long shipmentId;
    private String itemName;
    private Double weightKg;
    private Double volumeCbm;
    private Double lengthCm;
    private Double widthCm;
    private Double heightCm;
    private Integer quantity;
    private Boolean fragile;
    private Boolean stackable;
}
