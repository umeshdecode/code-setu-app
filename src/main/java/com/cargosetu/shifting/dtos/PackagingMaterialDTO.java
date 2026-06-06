package com.cargosetu.shifting.dtos;

import lombok.Data;

@Data
public class PackagingMaterialDTO {
    private Long id;
    private String materialName;
    private String unit;
    private Boolean active;
}
