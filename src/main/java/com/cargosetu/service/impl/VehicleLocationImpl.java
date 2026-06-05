package com.cargosetu.service.impl;

import com.cargosetu.model.VehicleLocation;
import com.cargosetu.repo.VehicleLocationRepo;
import com.cargosetu.service.VehicleLocationServ;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class VehicleLocationImpl implements VehicleLocationServ {
    @Autowired
    private VehicleLocationRepo repo;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
@Override
    public VehicleLocation saveLocation(Long vehicleId, double lat, double lng) {
        VehicleLocation loc = new VehicleLocation();
        loc.setVehicleId(vehicleId);
        loc.setLatitude(lat);
        loc.setLongitude(lng);
        loc.setTimestamp(LocalDateTime.now());
     VehicleLocation saved = repo.save(loc);
    messagingTemplate.convertAndSend("/topic/vehicle/" + vehicleId, saved);
    return saved;

}
    @Override
    public VehicleLocation getLatestLocation(Long vehicleId) {
        return repo.findLatestLocationToday(vehicleId)
                .orElse(null);
    }
}
