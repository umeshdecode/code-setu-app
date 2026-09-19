package com.cargosetu.loadmatch.service;

import com.cargosetu.loadmatch.dtos.TripCapacityDTO;
import com.cargosetu.loadmatch.dtos.TripStopDTO;
import com.cargosetu.utils.GenericDataDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface TripCapacityService {

    GenericDataDTO getCapacity(Long tripId);

    /**
     * Phase 0 baseline decision: seeds total/remaining capacity from the trip's
     * assigned Vehicle. Remaining starts equal to total (i.e. assumes the trip
     * is empty) because existing Shifting/ShiftInventoryItem records have no
     * weight/volume to deduct from — staff must call updateCapacity afterward to
     * declare how much of that capacity the trip's existing cargo already uses.
     */
    GenericDataDTO initializeCapacity(Long tripId);

    GenericDataDTO updateCapacity(Long tripId, TripCapacityDTO capacityDTO);

    GenericDataDTO getStops(Long tripId);

    GenericDataDTO addStop(Long tripId, TripStopDTO stopDTO);

    GenericDataDTO removeStop(Long tripId, Long stopId);

    GenericDataDTO reorderStops(Long tripId, List<Long> orderedStopIds);
}
