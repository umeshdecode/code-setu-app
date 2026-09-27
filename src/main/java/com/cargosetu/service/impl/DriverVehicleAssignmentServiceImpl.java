package com.cargosetu.service.impl;

import com.cargosetu.dto.AssignedDriverToVehicleDto;
import com.cargosetu.handler.CustomValidationException;
import com.cargosetu.model.Driver;
import com.cargosetu.model.DriverVehicleAssignment;
import com.cargosetu.model.Vehicle;
import com.cargosetu.repo.DriverRepository;
import com.cargosetu.repo.DriverVehicleAssignmentRepository;
import com.cargosetu.repo.VehicleRepository;
import com.cargosetu.service.DriverVehicleAssignmentService;
import com.cargosetu.utils.CommonStatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DriverVehicleAssignmentServiceImpl implements DriverVehicleAssignmentService {

    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_DRIVER_ASSIGNED = "ASSIGNED";
    private static final String STATUS_VEHICLE_ON_TRIP = "ON_TRIP";

    @Autowired
    private DriverVehicleAssignmentRepository repository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Override
    @Transactional
    public DriverVehicleAssignment assignDriver(Long driverId, Long vehicleId) {
        log.info("Assigning driver {} to vehicle {}", driverId, vehicleId);

        // Lock ordering: always Driver before Vehicle, to avoid deadlocks with unassign().
        Driver driver = driverRepository.findByIdForUpdate(driverId)
                .orElseThrow(() -> new CustomValidationException(CommonStatusCode.NOT_FOUND,
                        "Driver not found with id: " + driverId));

        Vehicle vehicle = vehicleRepository.findByIdForUpdate(vehicleId)
                .orElseThrow(() -> new CustomValidationException(CommonStatusCode.NOT_FOUND,
                        "Vehicle not found with id: " + vehicleId));

        if (!STATUS_AVAILABLE.equals(driver.getStatus())) {
            throw new CustomValidationException(CommonStatusCode.CONFLICT,
                    "Driver " + driverId + " is not AVAILABLE (current status: " + driver.getStatus() + ")");
        }

        if (!STATUS_AVAILABLE.equals(vehicle.getStatus())) {
            throw new CustomValidationException(CommonStatusCode.CONFLICT,
                    "Vehicle " + vehicleId + " is not AVAILABLE (current status: " + vehicle.getStatus() + ")");
        }

        List<DriverVehicleAssignment> driverActive = repository.findByDriverIdAndActiveTrue(driverId);
        if (!driverActive.isEmpty()) {
            throw new CustomValidationException(CommonStatusCode.CONFLICT,
                    "Driver " + driverId + " already has an active assignment (assignment id "
                            + driverActive.get(0).getId() + "). Unassign it first.");
        }

        List<DriverVehicleAssignment> vehicleActive = repository.findByVehicleIdAndActiveTrue(vehicleId);
        if (!vehicleActive.isEmpty()) {
            throw new CustomValidationException(CommonStatusCode.CONFLICT,
                    "Vehicle " + vehicleId + " already has an active assignment (assignment id "
                            + vehicleActive.get(0).getId() + "). Unassign it first.");
        }

        DriverVehicleAssignment assignment = new DriverVehicleAssignment();
        assignment.setDriver(driver);
        assignment.setVehicle(vehicle);
        assignment.setActive(true);

        driver.setStatus(STATUS_DRIVER_ASSIGNED);
        vehicle.setStatus(STATUS_VEHICLE_ON_TRIP);

        try {
            driverRepository.save(driver);
            vehicleRepository.save(vehicle);
            DriverVehicleAssignment saved = repository.save(assignment);
            log.info("Assigned driver {} to vehicle {} as assignment {}", driverId, vehicleId, saved.getId());
            return saved;
        } catch (DataIntegrityViolationException ex) {
            // Defense-in-depth: the DB-level unique constraint caught a race the
            // app-level checks above missed.
            log.warn("DB constraint rejected duplicate active assignment for driver {} / vehicle {}: {}",
                    driverId, vehicleId, ex.getMessage());
            throw new CustomValidationException(CommonStatusCode.CONFLICT,
                    "Driver or vehicle already has an active assignment", ex);
        }
    }

    @Override
    public List<AssignedDriverToVehicleDto> getAllAssignments() {
        try {
            log.info("Fetching all assignments");
            List<DriverVehicleAssignment> assignmentList = repository.findAllByOrderByIdDesc();
            List<AssignedDriverToVehicleDto> driverToVehicleDtos = new ArrayList<>();
            for (DriverVehicleAssignment assignment : assignmentList) {
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
    @Transactional
    public void unassign(Long assignmentId, String reason) {
        log.info("Unassigning assignment id {}", assignmentId);

        DriverVehicleAssignment assignment = repository.findByIdForUpdate(assignmentId)
                .orElseThrow(() -> new CustomValidationException(CommonStatusCode.NOT_FOUND,
                        "Assignment not found with id: " + assignmentId));

        if (Boolean.FALSE.equals(assignment.getActive())) {
            throw new CustomValidationException(CommonStatusCode.CONFLICT,
                    "Assignment " + assignmentId + " is already inactive");
        }

        Long driverId = assignment.getDriver().getId();
        Long vehicleId = assignment.getVehicle().getId();

        // Lock ordering: always Driver before Vehicle, matching assignDriver().
        Driver driver = driverRepository.findByIdForUpdate(driverId)
                .orElseThrow(() -> new CustomValidationException(CommonStatusCode.NOT_FOUND,
                        "Driver not found with id: " + driverId));

        Vehicle vehicle = vehicleRepository.findByIdForUpdate(vehicleId)
                .orElseThrow(() -> new CustomValidationException(CommonStatusCode.NOT_FOUND,
                        "Vehicle not found with id: " + vehicleId));

        assignment.setActive(false);
        assignment.setUnassignedAt(LocalDateTime.now());
        if (reason != null && !reason.isBlank()) {
            assignment.setRemarks(reason);
        }
        repository.save(assignment);

        List<DriverVehicleAssignment> otherDriverActive =
                repository.findByDriverIdAndActiveTrueExcludingId(driverId, assignmentId);
        if (otherDriverActive.isEmpty() && STATUS_DRIVER_ASSIGNED.equals(driver.getStatus())) {
            driver.setStatus(STATUS_AVAILABLE);
            driverRepository.save(driver);
        }

        List<DriverVehicleAssignment> otherVehicleActive =
                repository.findByVehicleIdAndActiveTrueExcludingId(vehicleId, assignmentId);
        if (otherVehicleActive.isEmpty() && STATUS_VEHICLE_ON_TRIP.equals(vehicle.getStatus())) {
            vehicle.setStatus(STATUS_AVAILABLE);
            vehicleRepository.save(vehicle);
        }

        log.info("Unassigned assignment {} (driver {}, vehicle {})", assignmentId, driverId, vehicleId);
    }
}
