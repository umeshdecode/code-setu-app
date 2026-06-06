package com.cargosetu.shifting.dtos;

import lombok.Data;

@Data
public class ShiftInventoryPackagingDTO {
    private Long id;
    private Long inventoryItemId; // Link to the inventory item
    private Long packagingMaterialId; // Link to the type of packaging material
    private Double quantityUsed;

    private String materialName;
    private String unit;
}
