package com.cargosetu.loadmatch.service;

import com.cargosetu.loadmatch.dtos.TripShipmentDTO;
import com.cargosetu.utils.GenericDataDTO;
import org.springframework.stereotype.Service;

@Service
public interface TripShipmentService {

    /**
     * Lists a trip's manifest (TripShipment rows), ordered by sequenceOrder
     * then tripShipmentId.
     */
    GenericDataDTO getManifestForTrip(Long tripId);

    /**
     * Updates a manifest entry's manifestStatus and/or sequenceOrder. If the
     * manifestStatus is being changed to CANCELLED, releases the entry's
     * allocated capacity back onto the Trip immediately (the doc's explicit
     * "cancellation must release capacity immediately" rule), using the same
     * @Version-aware optimistic-locking pattern as acceptMatch.
     */
    GenericDataDTO updateManifestEntry(Long tripId, Long tripShipmentId, TripShipmentDTO updateDTO);
}
