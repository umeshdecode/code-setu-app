package com.cargosetu.service.impl;

import com.cargosetu.model.Driver;
import com.cargosetu.model.DriverVehicleAssignment;
import com.cargosetu.model.Vehicle;
import com.cargosetu.repo.DriverRepository;
import com.cargosetu.repo.DriverVehicleAssignmentRepository;
import com.cargosetu.repo.VehicleRepository;
import com.cargosetu.service.DriverVehicleAssignmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class DriverVehicleAssignmentServiceImpl implements DriverVehicleAssignmentService {


    @Autowired

    private DriverVehicleAssignmentRepository repository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Override
    public DriverVehicleAssignment assignDriver(Long driverId, Long vehicleId) {
        try {
            log.info("Assigning driver {} to vehicle {}", driverId, vehicleId);

            Driver driver = driverRepository.findById(driverId)
                    .orElseThrow(() -> new RuntimeException("Driver not found"));

            Vehicle vehicle = vehicleRepository.findById(vehicleId)
                    .orElseThrow(() -> new RuntimeException("Vehicle not found"));

            // 🚨 critical logic: deactivate old assignments
            List<DriverVehicleAssignment> existing =
                    repository.findByDriverIdAndActiveTrue(driverId);

            existing.forEach(a -> {
                a.setActive(false);
                a.setUnassignedAt(LocalDateTime.now());
            });

            repository.saveAll(existing);

            DriverVehicleAssignment assignment = new DriverVehicleAssignment();
            assignment.setDriverId(driver);
            assignment.setVehicleId(vehicle);
            assignment.setAssignedAt(LocalDateTime.now());
            assignment.setActive(true);

            return repository.save(assignment);

        } catch (Exception e) {
            log.error("Error assigning driver {} to vehicle {}: {}", driverId, vehicleId, e.getMessage(), e);
            throw e;
        }
    }
@Override
    public List<DriverVehicleAssignment> getAllAssignments() {
        try {
            log.info("Fetching all assignments");
            return repository.findAll();
        } catch (Exception e) {
            log.error("Error fetching assignments: {}", e.getMessage(), e);
            throw e;
        }
    }
    @Override
    public void unassign(Long assignmentId) {
        try {
            log.info("Unassigning assignment id {}", assignmentId);

            DriverVehicleAssignment assignment = repository.findById(assignmentId)
                    .orElseThrow(() -> new RuntimeException("Assignment not found"));

            assignment.setActive(false);
            assignment.setUnassignedAt(LocalDateTime.now());

            repository.save(assignment);

        } catch (Exception e) {
            log.error("Error unassigning {}", assignmentId, e.getMessage(), e);
            throw e;
        }
    }
}
