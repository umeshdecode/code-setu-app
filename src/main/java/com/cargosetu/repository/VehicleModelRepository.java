package com.cargosetu.repository;

import com.cargosetu.entity.VehicleBrand;
import com.cargosetu.entity.VehicleModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {
    Optional<VehicleModel> findByBrandAndSlug(VehicleBrand brand, String slug);
    Optional<VehicleModel> findByBrandSlugAndSlug(String brandSlug, String slug);
    Optional<VehicleModel> findBySourceAndSourceProductId(String source, String sourceProductId);
    List<VehicleModel> findByBrandAndActiveTrueOrderByNameAsc(VehicleBrand brand);
    List<VehicleModel> findByBrandSlugAndActiveTrueOrderByNameAsc(String brandSlug);
    List<VehicleModel> findByActiveTrueOrderByNameAsc();
    List<VehicleModel> findByNameContainingIgnoreCaseAndActiveTrue(String keyword);
    List<VehicleModel> findByBrandSlug(String brandSlug);
}
