package com.cargosetu.service;

import com.cargosetu.dto.AssignedDriverToVehicleDto;
import com.cargosetu.model.DriverVehicleAssignment;

import java.awt.print.Pageable;
import java.util.List;

public interface DriverVehicleAssignmentService {
    public void unassign(Long assignmentId) ;
    public List<AssignedDriverToVehicleDto> getAllAssignments() ;
    public DriverVehicleAssignment assignDriver(Long driverId, Long vehicleId) ;
    }
