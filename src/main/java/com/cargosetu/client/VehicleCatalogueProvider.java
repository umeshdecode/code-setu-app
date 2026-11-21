package com.cargosetu.client;

import com.cargosetu.client.model.VehicleBrandData;
import com.cargosetu.client.model.VehicleModelData;

import java.util.List;

public interface VehicleCatalogueProvider {
    List<VehicleBrandData> getBrands();
    List<VehicleModelData> getModels(String brandSlug);
    VehicleModelData getModel(String brandSlug, String modelSlug);
}
