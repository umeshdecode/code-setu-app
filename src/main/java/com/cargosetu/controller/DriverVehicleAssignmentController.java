package com.cargosetu.controller;

import com.cargosetu.dto.AssignedDriverToVehicleDto;
import com.cargosetu.model.DriverVehicleAssignment;
import com.cargosetu.service.DriverVehicleAssignmentService;
import com.cargosetu.utils.BaseURL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(BaseURL.BASE_URL+BaseURL.ASSIGNMENTS)
public class DriverVehicleAssignmentController {

    @Autowired
    private DriverVehicleAssignmentService service;

    @PostMapping
    public ResponseEntity<DriverVehicleAssignment> assign(
            @RequestParam Long driverId,
            @RequestParam Long vehicleId) {

        return ResponseEntity.ok(service.assignDriver(driverId, vehicleId));
    }

    @GetMapping
    public ResponseEntity<List<AssignedDriverToVehicleDto>> getAll() {
        return ResponseEntity.ok(service.getAllAssignments());
    }

    @PutMapping("/unassign/{id}")
    public ResponseEntity<String> unassign(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        service.unassign(id, reason);
        return ResponseEntity.ok("Unassigned successfully");
    }
}
