package com.cargosetu.maps.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteLegDTO {
    private Long distanceMeters;
    private Long durationSeconds;
}
