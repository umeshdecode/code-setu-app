package com.cargosetu.shifting.dtos;

import lombok.Data;

@Data
public class ShiftInventoryPhotoDTO {
    private Long id;
    private Long inventoryItemId; // Link to the inventory item
    private String photoUrl;
    private String photoType;
}
