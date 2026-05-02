package com.cargosetu.service.impl;

import com.cargosetu.dto.VehicleDto;
import com.cargosetu.model.Vehicle;
import com.cargosetu.repo.VehicleRepository;
import com.cargosetu.service.VehicleService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class VehicleServiceImpl implements VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Override
    public Vehicle createVehicle(VehicleDto vehicleDto) {
        try {
            log.info("Creating vehicle with registration: {}", vehicleDto.getRegistrationNumber());
            Vehicle vehicle = new Vehicle(vehicleDto);
            vehicle.setCreatedAt(LocalDateTime.now());
            vehicle.setUpdatedAt(LocalDateTime.now());

            return vehicleRepository.save(vehicle);

        } catch (Exception e) {
            log.error("Error while creating vehicle: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create vehicle");
        }
    }

    @Override
    public Vehicle getVehicle(Long id) {
        try {
            log.info("Fetching vehicle with id: {}", id);

            return vehicleRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        } catch (Exception e) {
            log.error("Error fetching vehicle id {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Vehicle> getAllVehicles() {
        try {
            log.info("Fetching all vehicles");
            return vehicleRepository.findAll();
        } catch (Exception e) {
            log.error("Error fetching vehicles: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Vehicle updateVehicle(Long id, VehicleDto updatedVehicle) {
        try {
            log.info("Updating vehicle id: {}", id);

            Vehicle vehicle = getVehicle(id);

            vehicle.setVehicleType(updatedVehicle.getVehicleType());
            vehicle.setModel(updatedVehicle.getModel());
            vehicle.setManufacturer(updatedVehicle.getManufacturer());
            vehicle.setCapacityKg(updatedVehicle.getCapacityKg());
            vehicle.setStatus(updatedVehicle.getStatus());
            vehicle.setUpdatedAt(LocalDateTime.now());

            return vehicleRepository.save(vehicle);

        } catch (Exception e) {
            log.error("Error updating vehicle id {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }
@Override
public void deleteVehicle(Long id) {
        try {
            log.info("Deleting vehicle id: {}", id);

            Vehicle vehicle = getVehicle(id);
            vehicleRepository.delete(vehicle);

        } catch (Exception e) {
            log.error("Error deleting vehicle id {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}
