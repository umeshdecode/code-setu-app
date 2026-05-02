package com.cargosetu.service;

import com.cargosetu.model.VehicleLocation;

public interface VehicleLocationServ {
    public VehicleLocation saveLocation(Long vehicleId, double lat, double lng);
    public VehicleLocation getLatestLocation(Long vehicleId);

    }
