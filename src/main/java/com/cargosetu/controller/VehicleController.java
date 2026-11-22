package com.cargosetu.controller;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.dto.TruckOptionsDto;
import com.cargosetu.dto.VehicleDto;
import com.cargosetu.model.Vehicle;
import com.cargosetu.repo.VehicleRepository;
import com.cargosetu.service.VehicleService;
import com.cargosetu.utils.BaseURL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Objects;

/**
 * Vehicles are company-scoped: every call uses the company of the logged-in user, so a company
 * only ever sees, creates, edits or deletes its own fleet.
 */
@RestController
@RequestMapping(BaseURL.VEHICLE)
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private VehicleRepository vehicleRepository;

    @PostMapping
    public ResponseEntity<Vehicle> create(@AuthenticationPrincipal LoggedInUser user, @RequestBody VehicleDto vehicle) {
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Vehicle created = vehicleService.createVehicle(vehicle);
        // The DTO carries no company; take it from the authenticated user.
        created.setCompanyId(user.getCompanyId());
        return ResponseEntity.ok(vehicleRepository.save(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vehicle> get(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id) {
        Vehicle vehicle = vehicleRepository.findById(id).orElse(null);
        if (!ownedBy(vehicle, user)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(vehicle);
    }

    /** Only the caller's company's vehicles (was: every vehicle of every company). */
    @GetMapping
    public ResponseEntity<List<Vehicle>> getAll(@AuthenticationPrincipal LoggedInUser user) {
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(vehicleService.getVehiclesByCompany(user.getCompanyId()));
    }

    @GetMapping("/options")
    public ResponseEntity<List<TruckOptionsDto>> getOptions(@RequestParam(required = false) String truckType) {
        return ResponseEntity.ok(vehicleService.getTruckOptions(truckType));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vehicle> update(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id, @RequestBody VehicleDto vehicle) {
        if (!ownedBy(vehicleRepository.findById(id).orElse(null), user)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(vehicleService.updateVehicle(id, vehicle));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id) {
        if (!ownedBy(vehicleRepository.findById(id).orElse(null), user)) {
            return ResponseEntity.notFound().build();
        }
        vehicleService.deleteVehicle(id);
        return ResponseEntity.ok("Deleted successfully");
    }

    private static boolean ownedBy(Vehicle vehicle, LoggedInUser user) {
        return vehicle != null && user != null && user.getCompanyId() != null
                && Objects.equals(vehicle.getCompanyId(), user.getCompanyId());
    }
}
