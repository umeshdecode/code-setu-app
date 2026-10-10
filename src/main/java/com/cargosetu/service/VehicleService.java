package com.cargosetu.service;

import com.cargosetu.dto.TruckOptionsDto;
import com.cargosetu.dto.VehicleDto;
import com.cargosetu.model.Vehicle;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface VehicleService {
    public Vehicle createVehicle(VehicleDto vehicle);

    public Vehicle getVehicle(Long id);

    public List<Vehicle> getAllVehicles();

    public Vehicle updateVehicle(Long id, VehicleDto updatedVehicle);

    public void deleteVehicle(Long id);

    public List<TruckOptionsDto> getTruckOptions(String truckType);
}
