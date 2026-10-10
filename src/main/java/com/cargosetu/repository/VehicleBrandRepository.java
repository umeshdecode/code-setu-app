package com.cargosetu.repository;

import com.cargosetu.entity.VehicleBrand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleBrandRepository extends JpaRepository<VehicleBrand, Long> {
    Optional<VehicleBrand> findBySlug(String slug);
    Optional<VehicleBrand> findByNameIgnoreCase(String name);
    List<VehicleBrand> findByActiveTrueOrderByNameAsc();
}
