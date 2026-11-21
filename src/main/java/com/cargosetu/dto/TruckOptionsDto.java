package com.cargosetu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TruckOptionsDto {
    private Long id;
    private String truckType; // e.g., OPEN_TRUCK, CONTAINER, LCV, TIPPER, TANKER, TRAILER
    private String displayName;
    private Boolean active;
    private List<String> bodyTypes; // e.g., ["LCV_OPEN", "LCV_CONTAINER"] or ["OPEN_FULL_BODY", "OPEN_HALF_BODY"]
    private List<String> availableLengthsFeet; // e.g., ["14 Feet", "17 Feet", "19 Feet", "32 Feet"]
    private List<String> availableWheelers; // e.g., ["4 Wheeler", "6 Wheeler", "14 Wheeler"]
    private List<String> availablePassingTons; // e.g., ["1-2 Ton", "7.5-10 Ton", "24-25 Ton"]
}
