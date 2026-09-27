package com.cargosetu.repo;

import com.cargosetu.model.DriverVehicleAssignment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverVehicleAssignmentRepository extends JpaRepository<DriverVehicleAssignment, Long> {

    @Query(value = "SELECT * FROM driver_vehicle_assignments WHERE driver_id = :driverId AND active = true",
            nativeQuery = true)
    List<DriverVehicleAssignment> findByDriverIdAndActiveTrue(@Param("driverId") Long driverId);

    @Query(value = "SELECT * FROM driver_vehicle_assignments WHERE vehicle_id = :vehicleId AND active = true",
            nativeQuery = true)
    List<DriverVehicleAssignment> findByVehicleIdAndActiveTrue(@Param("vehicleId") Long vehicleId);

    @Query(value = "SELECT * FROM driver_vehicle_assignments WHERE driver_id = :driverId AND active = true AND id <> :excludeId",
            nativeQuery = true)
    List<DriverVehicleAssignment> findByDriverIdAndActiveTrueExcludingId(@Param("driverId") Long driverId, @Param("excludeId") Long excludeId);

    @Query(value = "SELECT * FROM driver_vehicle_assignments WHERE vehicle_id = :vehicleId AND active = true AND id <> :excludeId",
            nativeQuery = true)
    List<DriverVehicleAssignment> findByVehicleIdAndActiveTrueExcludingId(@Param("vehicleId") Long vehicleId, @Param("excludeId") Long excludeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM DriverVehicleAssignment a WHERE a.id = :id")
    Optional<DriverVehicleAssignment> findByIdForUpdate(@Param("id") Long id);

    List<DriverVehicleAssignment> findAllByOrderByIdDesc();

}
