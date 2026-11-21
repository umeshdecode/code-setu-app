package com.cargosetu.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VehicleCatalogueResponseDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private VehicleBrandDto brand;
    private String name;
    private String slug;
    private String category;
    private String subCategory;
    private String seriesName;
    private String brochureUrl;
    private List<String> images;
    private VehicleSpecificationDto specifications;
}
