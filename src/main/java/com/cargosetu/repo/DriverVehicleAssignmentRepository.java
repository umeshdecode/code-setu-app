package com.cargosetu.repo;

import com.cargosetu.model.DriverVehicleAssignment;
import org.hibernate.query.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.awt.print.Pageable;
import java.util.List;

@Repository
public interface DriverVehicleAssignmentRepository extends JpaRepository<DriverVehicleAssignment, Long> {

    @Query(value = "SELECT * FROM driver_vehicle_assignments WHERE driver_id = :driverId AND active = true",
            nativeQuery = true)
    List<DriverVehicleAssignment> findByDriverIdAndActiveTrue(@Param("driverId") Long driverId);
    @Query(value = "SELECT * FROM driver_vehicle_assignments WHERE vehicle_id = :vehicleId AND active = true",
            nativeQuery = true)
    List<DriverVehicleAssignment> findByVehicleIdAndActiveTrue(@Param("vehicleId") Long vehicleId);}
