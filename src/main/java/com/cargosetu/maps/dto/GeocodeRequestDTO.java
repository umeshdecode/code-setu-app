package com.cargosetu.maps.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GeocodeRequestDTO {
    @NotBlank
    @Size(max = 300)
    private String query;
}
