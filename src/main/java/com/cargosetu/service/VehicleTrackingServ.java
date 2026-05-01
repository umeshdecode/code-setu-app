package com.cargosetu.service;

import com.cargosetu.model.VehicleTrack;
import com.cargosetu.repo.VehicleLocationRepo;

public interface VehicleTrackingServ {
    public VehicleTrack saveLocation(Long vehicleId, double lat, double lng);
    public VehicleTrack getLatestLocation(Long vehicleId);

    }
