package com.cargosetu.loadmatch.repository;

import com.cargosetu.loadmatch.entity.TripShipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripShipmentRepository extends JpaRepository<TripShipment, Long> {

    List<TripShipment> findByTrip_TripIdOrderBySequenceOrderAscTripShipmentIdAsc(Long tripId);

    List<TripShipment> findByShipment_ShipmentId(Long shipmentId);
}
