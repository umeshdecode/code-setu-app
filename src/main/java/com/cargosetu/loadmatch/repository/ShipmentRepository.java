package com.cargosetu.loadmatch.repository;

import com.cargosetu.loadmatch.entity.Shipment;
import com.cargosetu.loadmatch.entity.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Page<Shipment> findByIsDeletedOrderByShipmentIdDesc(Boolean isDeleted, Pageable pageable);

    Page<Shipment> findByCompanyIdAndIsDeletedOrderByShipmentIdDesc(Integer companyId, Boolean isDeleted, Pageable pageable);

    @Query("SELECT s FROM Shipment s WHERE s.isDeleted = false " +
            "AND s.status IN :statuses " +
            "AND (:sourceCity IS NULL OR LOWER(s.sourceCity) LIKE LOWER(CONCAT('%', :sourceCity, '%'))) " +
            "AND (:destinationCity IS NULL OR LOWER(s.destinationCity) LIKE LOWER(CONCAT('%', :destinationCity, '%'))) " +
            "AND (:vehicleTypeRequired IS NULL OR LOWER(s.vehicleTypeRequired) = LOWER(:vehicleTypeRequired)) " +
            "ORDER BY s.shipmentId DESC")
    Page<Shipment> searchOpen(@Param("sourceCity") String sourceCity,
                              @Param("destinationCity") String destinationCity,
                              @Param("vehicleTypeRequired") String vehicleTypeRequired,
                              @Param("statuses") List<ShipmentStatus> statuses,
                              Pageable pageable);
}
