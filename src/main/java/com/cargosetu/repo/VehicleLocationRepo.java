package com.cargosetu.repo;

import com.cargosetu.model.VehicleTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleLocationRepo extends JpaRepository<VehicleTrack,Long > {
    Optional<VehicleTrack> findTopByVehicleIdOrderByTimestampDesc(Long vehicleId);
    @Query(value = "SELECT * FROM tblmvahicletrack t " +
            "WHERE t.id = :vehicleId " +
            "AND t.timestamp > CURRENT_DATE() " +
            "ORDER BY t.timestamp DESC LIMIT 1",
            nativeQuery = true)
    Optional<VehicleTrack> findLatestLocationToday(@Param("vehicleId") Long vehicleId);


}
