package com.cargosetu.shifting.dtos;

import lombok.Data;

import java.util.List;

@Data
public class ShiftInventoryItemDTO {
    private Long id;
    private Long shiftId; // Link to the main Shifting record
    private String roomName;
    private String itemName;
    private String category;
    private String packageNumber;
    private String remarks;
    private String conditionStatus;
    private Boolean packed;
    private Boolean loaded;
    private Boolean unloaded;

    // Nested DTOs for related data
    private List<ShiftInventoryPhotoDTO> photos;
    private List<ShiftInventoryPackagingDTO> packagingDetails;
}
