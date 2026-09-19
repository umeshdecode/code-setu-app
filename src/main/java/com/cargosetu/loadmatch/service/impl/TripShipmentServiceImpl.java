package com.cargosetu.loadmatch.service.impl;

import com.cargosetu.loadmatch.dtos.TripShipmentDTO;
import com.cargosetu.loadmatch.entity.ManifestStatus;
import com.cargosetu.loadmatch.entity.TripShipment;
import com.cargosetu.loadmatch.mapper.TripShipmentMapper;
import com.cargosetu.loadmatch.repository.TripShipmentRepository;
import com.cargosetu.loadmatch.service.TripShipmentService;
import com.cargosetu.model.Trip;
import com.cargosetu.repo.TripRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TripShipmentServiceImpl implements TripShipmentService {

    private final TripRepository tripRepository;
    private final TripShipmentRepository tripShipmentRepository;
    private final TripShipmentMapper tripShipmentMapper;

    public TripShipmentServiceImpl(TripRepository tripRepository,
                                    TripShipmentRepository tripShipmentRepository,
                                    TripShipmentMapper tripShipmentMapper) {
        this.tripRepository = tripRepository;
        this.tripShipmentRepository = tripShipmentRepository;
        this.tripShipmentMapper = tripShipmentMapper;
    }

    @Override
    public GenericDataDTO getManifestForTrip(Long tripId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            List<TripShipment> manifest = tripShipmentRepository
                    .findByTrip_TripIdOrderBySequenceOrderAscTripShipmentIdAsc(tripId);
            List<TripShipmentDTO> dtos = manifest.stream().map(tripShipmentMapper::toDto).collect(Collectors.toList());

            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Trip manifest fetched successfully!");
            response.setData(dtos);
        } catch (Exception e) {
            log.error("Error fetching manifest for trip ID {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while fetching the trip manifest.");
            response.setErrors(new Object[]{e.getMessage()});
        }
        return response;
    }

    /**
     * Capacity-release path: this is also a Trip capacity mutation, so it
     * uses the same @Version-aware optimistic-locking pattern as
     * LoadMatchingServiceImpl.acceptMatch - re-fetch the Trip fresh inside
     * this transaction, mutate remaining capacity, then saveAndFlush (not
     * save) so a concurrent modification surfaces as
     * ObjectOptimisticLockingFailureException right here instead of at
     * commit time, where it could no longer be caught and turned into a
     * structured response.
     */
    @Override
    @Transactional
    public GenericDataDTO updateManifestEntry(Long tripId, Long tripShipmentId, TripShipmentDTO updateDTO) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<TripShipment> tripShipmentOptional = tripShipmentRepository.findById(tripShipmentId);
            if (tripShipmentOptional.isEmpty() || tripShipmentOptional.get().getTrip() == null
                    || !tripShipmentOptional.get().getTrip().getTripId().equals(tripId)) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Manifest entry not found with ID: " + tripShipmentId + " for trip ID: " + tripId);
                return response;
            }
            TripShipment tripShipment = tripShipmentOptional.get();
            ManifestStatus currentStatus = tripShipment.getManifestStatus();
            ManifestStatus newStatus = updateDTO.getManifestStatus() != null ? updateDTO.getManifestStatus() : currentStatus;

            if (updateDTO.getSequenceOrder() != null) {
                tripShipment.setSequenceOrder(updateDTO.getSequenceOrder());
            }

            boolean releasingCapacity = newStatus == ManifestStatus.CANCELLED && currentStatus != ManifestStatus.CANCELLED;

            if (releasingCapacity) {
                Optional<Trip> tripOptional = tripRepository.findById(tripId);
                if (tripOptional.isEmpty()) {
                    response.setResponseCode(CommonStatusCode.NOT_FOUND);
                    response.setResponseMessege("Trip not found with ID: " + tripId);
                    return response;
                }
                Trip trip = tripOptional.get();
                double releaseWeight = tripShipment.getAllocatedWeightKg() != null ? tripShipment.getAllocatedWeightKg() : 0d;
                double releaseVolume = tripShipment.getAllocatedVolumeCbm() != null ? tripShipment.getAllocatedVolumeCbm() : 0d;
                double currentRemainingWeight = trip.getRemainingWeightKg() != null ? trip.getRemainingWeightKg() : 0d;
                double currentRemainingVolume = trip.getRemainingVolumeCbm() != null ? trip.getRemainingVolumeCbm() : 0d;
                trip.setRemainingWeightKg(currentRemainingWeight + releaseWeight);
                trip.setRemainingVolumeCbm(currentRemainingVolume + releaseVolume);

                try {
                    tripRepository.saveAndFlush(trip);
                } catch (ObjectOptimisticLockingFailureException e) {
                    log.warn("Optimistic lock conflict releasing capacity for trip ID {} (manifest entry {}): {}",
                            tripId, tripShipmentId, e.getMessage());
                    response.setResponseCode(CommonStatusCode.CONFLICT);
                    response.setResponseMessege("Trip capacity was updated concurrently by another action. Please refresh and try again.");
                    return response;
                }
            }

            tripShipment.setManifestStatus(newStatus);
            tripShipment = tripShipmentRepository.save(tripShipment);

            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Manifest entry updated successfully.");
            response.setData(tripShipmentMapper.toDto(tripShipment));
        } catch (Exception e) {
            log.error("Error updating manifest entry {} for trip ID {}: {}", tripShipmentId, tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while updating the manifest entry.");
            response.setErrors(new Object[]{e.getMessage()});
        }
        return response;
    }
}
