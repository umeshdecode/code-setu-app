package com.cargosetu.repo;

import com.cargosetu.model.VehicleLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleLocationRepo extends JpaRepository<VehicleLocation,Long > {
    Optional<VehicleLocation> findTopByVehicleIdOrderByTimestampDesc(Long vehicleId);
    @Query(value = "SELECT * FROM tblmvahicletrack t " +
            "WHERE t.vehicle_id = :vehicleId " +
            "AND t.timestamp > CURRENT_DATE() " +
            "ORDER BY t.timestamp DESC LIMIT 1",
            nativeQuery = true)
    Optional<VehicleLocation> findLatestLocationToday(@Param("vehicleId") Long vehicleId);


}
