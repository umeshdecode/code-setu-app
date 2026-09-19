package com.cargosetu.loadmatch.repository;

import com.cargosetu.loadmatch.entity.ShipmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipmentItemRepository extends JpaRepository<ShipmentItem, Long> {
    List<ShipmentItem> findByShipment_ShipmentId(Long shipmentId);
}
