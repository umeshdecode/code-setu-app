package com.cargosetu.controller;

import com.cargosetu.model.VehicleTrack;
import com.cargosetu.service.VehicleTrackingServ;
import com.cargosetu.service.impl.VehicleTrackingImpl;
import com.cargosetu.utils.BaseURL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(BaseURL.BASE_URL+BaseURL.VEHICLE)

public class VehicleTrackingController {

    @Autowired
    private VehicleTrackingServ service;

    @PostMapping("/location")
    public ResponseEntity<?> updateLocation(@RequestBody Map<String, Object> body) {

        Long vehicleId = Long.valueOf(body.get("vehicleId").toString());
        double lat = Double.parseDouble(body.get("lat").toString());
        double lng = Double.parseDouble(body.get("lng").toString());

        VehicleTrack loc = service.saveLocation(vehicleId, lat, lng);

        return ResponseEntity.ok(loc);
    }

    @GetMapping("/{vehicleId}/location")
    public ResponseEntity<?> getLocation(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(service.getLatestLocation(vehicleId));
    }
}
