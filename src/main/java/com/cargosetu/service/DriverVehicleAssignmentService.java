package com.cargosetu.service;

import com.cargosetu.model.DriverVehicleAssignment;

import java.util.List;

public interface DriverVehicleAssignmentService {
    public void unassign(Long assignmentId) ;
    public List<DriverVehicleAssignment> getAllAssignments() ;
    public DriverVehicleAssignment assignDriver(Long driverId, Long vehicleId) ;
    }
