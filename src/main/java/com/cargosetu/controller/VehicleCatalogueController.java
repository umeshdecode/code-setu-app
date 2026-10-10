package com.cargosetu.controller;

import com.cargosetu.dto.VehicleBrandDto;
import com.cargosetu.dto.VehicleCatalogueResponseDto;
import com.cargosetu.dto.VehicleModelDto;
import com.cargosetu.service.VehicleCatalogueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/cargosetu/vehicles")
@RequiredArgsConstructor
public class VehicleCatalogueController {

    private final VehicleCatalogueService catalogueService;

    /**
     * Get active vehicle brands sorted A-Z.
     */
    @GetMapping("/brands")
    public ResponseEntity<List<VehicleBrandDto>> getBrands() {
        return ResponseEntity.ok(catalogueService.getBrands());
    }

    /**
     * Get models for a specific brand slug (e.g. ?brand=tata).
     */
    @GetMapping("/models")
    public ResponseEntity<List<VehicleModelDto>> getModels(@RequestParam(name = "brand", required = false) String brand) {
        return ResponseEntity.ok(catalogueService.getModelsByBrand(brand));
    }

    /**
     * Get full model and specifications details by model ID.
     */
    @GetMapping("/models/{id}")
    public ResponseEntity<VehicleCatalogueResponseDto> getModelById(@PathVariable Long id) {
        return ResponseEntity.ok(catalogueService.getModelDetails(id));
    }

    /**
     * Search models by keyword (e.g. ?keyword=intra).
     */
    @GetMapping("/models/search")
    public ResponseEntity<List<VehicleModelDto>> searchModels(@RequestParam(name = "keyword", required = false) String keyword) {
        return ResponseEntity.ok(catalogueService.searchModels(keyword));
    }
}
