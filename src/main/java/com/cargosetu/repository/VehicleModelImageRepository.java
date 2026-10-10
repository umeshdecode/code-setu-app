package com.cargosetu.repository;

import com.cargosetu.entity.VehicleModel;
import com.cargosetu.entity.VehicleModelImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleModelImageRepository extends JpaRepository<VehicleModelImage, Long> {
    List<VehicleModelImage> findByVehicleModelOrderByDisplayOrderAsc(VehicleModel vehicleModel);
    void deleteByVehicleModel(VehicleModel vehicleModel);
}
