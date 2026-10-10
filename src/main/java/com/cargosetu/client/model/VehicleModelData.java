package com.cargosetu.client.model;

import com.cargosetu.client.trucksfloor.TrucksfloorProduct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleModelData {
    private String name;
    private String slug;
    private String brandSlug;
    private String brandVehicleName;
    private String primarySlug;
    private String category;
    private String subCategory;
    private String seriesName;
    private String seriesSlug;
    private String brochureUrl;
    private String source;
    private String sourceProductId;
    private List<String> imageUrls;
    private TrucksfloorProduct rawProduct;
}
