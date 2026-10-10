package com.cargosetu.controller;

import com.cargosetu.dto.TruckOptionsDto;
import com.cargosetu.service.TruckOptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/truck-options")
@RequiredArgsConstructor
public class AdminTruckOptionsController {

    private final TruckOptionService truckOptionService;

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'COMPANY_ADMIN')")
    @GetMapping
    public ResponseEntity<List<TruckOptionsDto>> getAllOptions() {
        return ResponseEntity.ok(truckOptionService.getAllTruckOptions());
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'COMPANY_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<TruckOptionsDto> getOptionById(@PathVariable Long id) {
        return ResponseEntity.ok(truckOptionService.getTruckOptionById(id));
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'COMPANY_ADMIN')")
    @PostMapping
    public ResponseEntity<TruckOptionsDto> createOption(@RequestBody TruckOptionsDto dto) {
        return ResponseEntity.ok(truckOptionService.createTruckOption(dto));
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'COMPANY_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<TruckOptionsDto> updateOption(@PathVariable Long id, @RequestBody TruckOptionsDto dto) {
        return ResponseEntity.ok(truckOptionService.updateTruckOption(id, dto));
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'COMPANY_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteOption(@PathVariable Long id) {
        truckOptionService.deleteTruckOption(id);
        return ResponseEntity.ok("Truck option deleted successfully");
    }
}
