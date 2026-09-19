package com.cargosetu.loadmatch.repository;

import com.cargosetu.loadmatch.entity.Shipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Page<Shipment> findByIsDeletedOrderByShipmentIdDesc(Boolean isDeleted, Pageable pageable);

    Page<Shipment> findByCompanyIdAndIsDeletedOrderByShipmentIdDesc(Integer companyId, Boolean isDeleted, Pageable pageable);
}
