package com.cargosetu.client.trucksfloor;

import com.cargosetu.client.VehicleCatalogueProvider;
import com.cargosetu.client.model.VehicleBrandData;
import com.cargosetu.client.model.VehicleModelData;
import com.cargosetu.config.TrucksfloorProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrucksfloorVehicleCatalogueProvider implements VehicleCatalogueProvider {

    private final TrucksfloorClient trucksfloorClient;
    private final TrucksfloorProperties properties;

    @Override
    public List<VehicleBrandData> getBrands() {
        List<VehicleBrandData> brands = new ArrayList<>();
        for (String slug : properties.getSyncBrands()) {
            brands.add(VehicleBrandData.builder()
                    .slug(slug.toLowerCase().trim())
                    .name(capitalize(slug.trim()))
                    .source("trucksfloor")
                    .build());
        }
        return brands;
    }

    @Override
    public List<VehicleModelData> getModels(String brandSlug) {
        List<VehicleModelData> models = new ArrayList<>();
        int page = 1;
        while (true) {
            TrucksfloorResponse response = trucksfloorClient.getTrucks(brandSlug, page);
            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                break;
            }
            for (TrucksfloorProduct product : response.getData()) {
                models.add(mapToModelData(brandSlug, product));
            }
            if (response.getLastPage() == null || page >= response.getLastPage()) {
                break;
            }
            page++;
        }
        return models;
    }

    @Override
    public VehicleModelData getModel(String brandSlug, String modelSlug) {
        List<VehicleModelData> models = getModels(brandSlug);
        return models.stream()
                .filter(m -> modelSlug.equalsIgnoreCase(m.getSlug()))
                .findFirst()
                .orElse(null);
    }

    private VehicleModelData mapToModelData(String brandSlug, TrucksfloorProduct product) {
        return VehicleModelData.builder()
                .name(product.resolveModelName())
                .slug(product.getSlug())
                .brandSlug(brandSlug)
                .brandVehicleName(product.getBrandVehicleName())
                .primarySlug(product.getPrimarySlug())
                .category(product.getCategory())
                .subCategory(product.getSubCategory())
                .seriesName(product.getSeries())
                .seriesSlug(product.getSeriesSlug())
                .brochureUrl(product.extractBrochureUrl())
                .source("trucksfloor")
                .sourceProductId(product.resolveProductId())
                .imageUrls(product.extractImageUrls())
                .rawProduct(product)
                .build();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        String[] parts = str.split("[-_\\s]+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
