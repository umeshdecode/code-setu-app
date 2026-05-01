package com.cargosetu.service.impl;

import com.cargosetu.model.VehicleTrack;
import com.cargosetu.repo.VehicleLocationRepo;
import com.cargosetu.service.VehicleTrackingServ;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class VehicleTrackingImpl implements VehicleTrackingServ {
    @Autowired
    private VehicleLocationRepo repo;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
@Override
    public VehicleTrack saveLocation(Long vehicleId, double lat, double lng) {
        VehicleTrack loc = new VehicleTrack();
        loc.setVehicleId(vehicleId);
        loc.setLatitude(lat);
        loc.setLongitude(lng);
        loc.setTimestamp(LocalDateTime.now());
     VehicleTrack saved = repo.save(loc);
    messagingTemplate.convertAndSend("/topic/vehicle/" + vehicleId, saved);
    return saved;

}
    @Override
    public VehicleTrack getLatestLocation(Long vehicleId) {
        return repo.findLatestLocationToday(vehicleId)
                .orElse(null);
    }
}
