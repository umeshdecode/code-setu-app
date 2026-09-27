package com.cargosetu.repo;

import com.cargosetu.model.VehicleManufacturerModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleManufacturerModelRepository extends JpaRepository<VehicleManufacturerModel, Long> {

    List<VehicleManufacturerModel> findAllByOrderByManufacturerAscModelAsc();

    @Query("SELECT DISTINCT v.manufacturer FROM VehicleManufacturerModel v ORDER BY v.manufacturer")
    List<String> findDistinctManufacturers();

    List<VehicleManufacturerModel> findByManufacturerOrderByModel(@Param("manufacturer") String manufacturer);
}
