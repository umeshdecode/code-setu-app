package com.cargosetu.service.impl;

import com.cargosetu.dto.AssignedDriverToVehicleDto;
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
import java.util.ArrayList;
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

            // Driver.status is the employment status (ACTIVE / INACTIVE / SUSPENDED) and
            // Vehicle.status is the fleet status (AVAILABLE / ON_TRIP / MAINTENANCE / OUT_OF_SERVICE).
            // Whether they are assigned is tracked by the active DriverVehicleAssignment row,
            // so we validate here but never overwrite either status with "Assigned".
            if ("INACTIVE".equals(driver.getStatus()) || "SUSPENDED".equals(driver.getStatus())) {
                throw new IllegalStateException("Driver is " + driver.getStatus() + " and cannot be assigned");
            }
            if ("MAINTENANCE".equals(vehicle.getStatus()) || "OUT_OF_SERVICE".equals(vehicle.getStatus())) {
                throw new IllegalStateException("Vehicle is " + vehicle.getStatus() + " and cannot be assigned");
            }

            // 🚨 critical logic: deactivate old assignments
            List<DriverVehicleAssignment> existing =
                    repository.findByDriverIdAndActiveTrue(driverId);

            existing.forEach(a -> {
                a.setActive(false);
                a.setUnassignedAt(LocalDateTime.now());
            });

            repository.saveAll(existing);

            DriverVehicleAssignment assignment = new DriverVehicleAssignment();
            assignment.setDriver(driver);
            assignment.setVehicle(vehicle);
            assignment.setAssignedAt(LocalDateTime.now());
            assignment.setActive(true);
            return repository.save(assignment);

        } catch (Exception e) {
            log.error("Error assigning driver {} to vehicle {}: {}", driverId, vehicleId, e.getMessage(), e);
            throw e;
        }
    }
@Override
    public List<AssignedDriverToVehicleDto> getAllAssignments() {
        try {
            log.info("Fetching all assignments");
            List<DriverVehicleAssignment> assignmentList =  repository.findAllByOrderByIdDesc();
            List<AssignedDriverToVehicleDto> driverToVehicleDtos = new ArrayList<>();
            for (DriverVehicleAssignment assignment : assignmentList){
                AssignedDriverToVehicleDto assignedDriverToVehicleDto = new AssignedDriverToVehicleDto();
                assignedDriverToVehicleDto.setDriverName(assignment.getDriver().getName());
                assignedDriverToVehicleDto.setDriverId(assignment.getDriver().getId());
                assignedDriverToVehicleDto.setVehicleId(assignment.getVehicle().getId());
                assignedDriverToVehicleDto.setRegistrationNumber(assignment.getVehicle().getRegistrationNumber());
                assignedDriverToVehicleDto.setActive(assignment.getActive());
                assignedDriverToVehicleDto.setAssignedAt(assignment.getAssignedAt());
                assignedDriverToVehicleDto.setRemarks(assignment.getRemarks());
                assignedDriverToVehicleDto.setId(assignment.getId());
                driverToVehicleDtos.add(assignedDriverToVehicleDto);
            }
            return driverToVehicleDtos;
        } catch (Exception e) {
            log.error("Error fetching assignments: {}", e.getMessage(), e);
            throw e;
        }
    }
    @Override
    public void unassign(Long assignmentId, String discreption) {
        try {
            log.info("Unassigning assignment id {}", assignmentId);

            DriverVehicleAssignment assignment = repository.findById(assignmentId)
                    .orElseThrow(() -> new RuntimeException("Assignment not found"));

            if (assignment !=null) {
                Driver driver = driverRepository.findById(assignment.getDriver().getId())
                        .orElseThrow(() -> new RuntimeException("Driver not found"));

                Vehicle vehicle = vehicleRepository.findById(assignment.getVehicle().getId())
                        .orElseThrow(() -> new RuntimeException("Vehicle not found"));
                assignment.setActive(false);
                assignment.setUnassignedAt(LocalDateTime.now());

                repository.save(assignment);
                // Statuses are left untouched: un-assigning ends the assignment row only.

            }


        } catch (Exception e) {
            log.error("Error unassigning {}", assignmentId, e.getMessage(), e);
            throw e;
        }
    }
}
