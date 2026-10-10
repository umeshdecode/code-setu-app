package com.cargosetu.repository;

import com.cargosetu.entity.VehicleModel;
import com.cargosetu.entity.VehicleSpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleSpecificationRepository extends JpaRepository<VehicleSpecification, Long> {
    Optional<VehicleSpecification> findByVehicleModel(VehicleModel vehicleModel);
    Optional<VehicleSpecification> findByVehicleModelId(Long vehicleModelId);
}
