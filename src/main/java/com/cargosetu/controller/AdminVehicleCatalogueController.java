package com.cargosetu.controller;

import com.cargosetu.dto.VehicleCatalogueSyncResultDto;
import com.cargosetu.service.VehicleCatalogueSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/vehicle-catalogue")
@RequiredArgsConstructor
public class AdminVehicleCatalogueController {

    private final VehicleCatalogueSyncService syncService;

    /**
     * Synchronize a specific vehicle brand catalogue.
     */
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'COMPANY_ADMIN')")
    @PostMapping("/sync/{brandSlug}")
    public ResponseEntity<VehicleCatalogueSyncResultDto> syncBrand(@PathVariable String brandSlug) {
        return ResponseEntity.ok(syncService.syncBrand(brandSlug));
    }

    /**
     * Synchronize all configured vehicle brands catalogue.
     */
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'COMPANY_ADMIN')")
    @PostMapping("/sync")
    public ResponseEntity<List<VehicleCatalogueSyncResultDto>> syncAllBrands() {
        return ResponseEntity.ok(syncService.syncAllConfiguredBrands());
    }
}
