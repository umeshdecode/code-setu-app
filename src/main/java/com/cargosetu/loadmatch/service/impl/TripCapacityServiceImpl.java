package com.cargosetu.loadmatch.service.impl;

import com.cargosetu.loadmatch.dtos.TripCapacityDTO;
import com.cargosetu.loadmatch.dtos.TripStopDTO;
import com.cargosetu.loadmatch.entity.TripStop;
import com.cargosetu.loadmatch.mapper.TripStopMapper;
import com.cargosetu.loadmatch.repository.TripStopRepository;
import com.cargosetu.loadmatch.service.TripCapacityService;
import com.cargosetu.model.Trip;
import com.cargosetu.repo.TripRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TripCapacityServiceImpl implements TripCapacityService {

    private final TripRepository tripRepository;
    private final TripStopRepository tripStopRepository;
    private final TripStopMapper tripStopMapper;

    public TripCapacityServiceImpl(TripRepository tripRepository,
                                    TripStopRepository tripStopRepository,
                                    TripStopMapper tripStopMapper) {
        this.tripRepository = tripRepository;
        this.tripStopRepository = tripStopRepository;
        this.tripStopMapper = tripStopMapper;
    }

    @Override
    public GenericDataDTO getCapacity(Long tripId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<Trip> tripOptional = tripRepository.findById(tripId);
            if (tripOptional.isEmpty()) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Trip not found with ID: " + tripId);
                return response;
            }
            Trip trip = tripOptional.get();
            TripCapacityDTO capacityDTO = new TripCapacityDTO();
            capacityDTO.setTripId(trip.getTripId());
            capacityDTO.setRemainingWeightKg(trip.getRemainingWeightKg());
            capacityDTO.setRemainingVolumeCbm(trip.getRemainingVolumeCbm());
            capacityDTO.setTotalWeightKg(trip.getTotalWeightKg());
            capacityDTO.setTotalVolumeCbm(trip.getTotalVolumeCbm());
            capacityDTO.setVehicleCapacityKg(trip.getVehicleCapacityKg());
            capacityDTO.setVehicleCapacityCbm(trip.getVehicleCapacityCbm());
            capacityDTO.setVehicleTypeCategory(trip.getVehicleTypeCategory());
            capacityDTO.setEquipmentTags(trip.getEquipmentTags());

            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Trip capacity fetched successfully!");
            response.setData(capacityDTO);
        } catch (Exception e) {
            log.error("Error fetching capacity for trip ID {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while fetching trip capacity.");
        }
        return response;
    }

    @Override
    @Transactional
    public GenericDataDTO initializeCapacity(Long tripId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<Trip> tripOptional = tripRepository.findById(tripId);
            if (tripOptional.isEmpty()) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Trip not found with ID: " + tripId);
                return response;
            }
            Trip trip = tripOptional.get();
            if (trip.getVehicle() == null) {
                response.setResponseCode(CommonStatusCode.VALIDATION_ERROR);
                response.setResponseMessege("Trip has no assigned vehicle; assign a vehicle before initializing capacity.");
                return response;
            }

            Double vehicleCapacityKg = trip.getVehicle().getCapacityKg() == null
                    ? null : trip.getVehicle().getCapacityKg().doubleValue();
            Double vehicleCapacityCbm = trip.getVehicle().getCapacityCbm();

            trip.setVehicleCapacityKg(vehicleCapacityKg);
            trip.setVehicleCapacityCbm(vehicleCapacityCbm);
            trip.setVehicleTypeCategory(trip.getVehicle().getBodyType());
            trip.setEquipmentTags(trip.getVehicle().getEquipmentTags());
            trip.setTotalWeightKg(vehicleCapacityKg);
            trip.setTotalVolumeCbm(vehicleCapacityCbm);
            // Baseline assumption (Phase 0): remaining starts equal to total. If this
            // trip already carries cargo today, staff must follow up with
            // PUT /trip/{id}/capacity to declare how much capacity that consumes —
            // we never guess it, since legacy Shifting records carry no weight/volume.
            trip.setRemainingWeightKg(vehicleCapacityKg);
            trip.setRemainingVolumeCbm(vehicleCapacityCbm);

            Trip savedTrip = tripRepository.save(trip);
            TripCapacityDTO dto = new TripCapacityDTO();
            dto.setTripId(savedTrip.getTripId());
            dto.setRemainingWeightKg(savedTrip.getRemainingWeightKg());
            dto.setRemainingVolumeCbm(savedTrip.getRemainingVolumeCbm());
            dto.setTotalWeightKg(savedTrip.getTotalWeightKg());
            dto.setTotalVolumeCbm(savedTrip.getTotalVolumeCbm());
            dto.setVehicleCapacityKg(savedTrip.getVehicleCapacityKg());
            dto.setVehicleCapacityCbm(savedTrip.getVehicleCapacityCbm());
            dto.setVehicleTypeCategory(savedTrip.getVehicleTypeCategory());
            dto.setEquipmentTags(savedTrip.getEquipmentTags());

            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Trip capacity initialized from vehicle. Remaining capacity assumes the trip is empty until updated.");
            response.setData(dto);
        } catch (Exception e) {
            log.error("Error initializing capacity for trip ID {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while initializing trip capacity.");
        }
        return response;
    }

    @Override
    @Transactional
    public GenericDataDTO updateCapacity(Long tripId, TripCapacityDTO capacityDTO) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<Trip> tripOptional = tripRepository.findById(tripId);
            if (tripOptional.isEmpty()) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Trip not found with ID: " + tripId);
                return response;
            }
            Trip trip = tripOptional.get();
            if (capacityDTO.getRemainingWeightKg() != null) {
                trip.setRemainingWeightKg(capacityDTO.getRemainingWeightKg());
            }
            if (capacityDTO.getRemainingVolumeCbm() != null) {
                trip.setRemainingVolumeCbm(capacityDTO.getRemainingVolumeCbm());
            }
            if (capacityDTO.getTotalWeightKg() != null) {
                trip.setTotalWeightKg(capacityDTO.getTotalWeightKg());
            }
            if (capacityDTO.getTotalVolumeCbm() != null) {
                trip.setTotalVolumeCbm(capacityDTO.getTotalVolumeCbm());
            }
            if (capacityDTO.getVehicleCapacityKg() != null) {
                trip.setVehicleCapacityKg(capacityDTO.getVehicleCapacityKg());
            }
            if (capacityDTO.getVehicleCapacityCbm() != null) {
                trip.setVehicleCapacityCbm(capacityDTO.getVehicleCapacityCbm());
            }
            if (capacityDTO.getVehicleTypeCategory() != null) {
                trip.setVehicleTypeCategory(capacityDTO.getVehicleTypeCategory());
            }
            if (capacityDTO.getEquipmentTags() != null) {
                trip.setEquipmentTags(capacityDTO.getEquipmentTags());
            }

            Trip savedTrip = tripRepository.save(trip);
            TripCapacityDTO updatedDTO = new TripCapacityDTO();
            updatedDTO.setTripId(savedTrip.getTripId());
            updatedDTO.setRemainingWeightKg(savedTrip.getRemainingWeightKg());
            updatedDTO.setRemainingVolumeCbm(savedTrip.getRemainingVolumeCbm());
            updatedDTO.setTotalWeightKg(savedTrip.getTotalWeightKg());
            updatedDTO.setTotalVolumeCbm(savedTrip.getTotalVolumeCbm());
            updatedDTO.setVehicleCapacityKg(savedTrip.getVehicleCapacityKg());
            updatedDTO.setVehicleCapacityCbm(savedTrip.getVehicleCapacityCbm());
            updatedDTO.setVehicleTypeCategory(savedTrip.getVehicleTypeCategory());
            updatedDTO.setEquipmentTags(savedTrip.getEquipmentTags());

            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Trip capacity updated successfully!");
            response.setData(updatedDTO);
        } catch (Exception e) {
            log.error("Error updating capacity for trip ID {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while updating trip capacity.");
            response.setErrors(new Object[]{e.getMessage()});
        }
        return response;
    }

    @Override
    public GenericDataDTO getStops(Long tripId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            List<TripStop> stops = tripStopRepository.findByTrip_TripIdOrderBySequenceOrderAsc(tripId);
            List<TripStopDTO> stopDTOs = stops.stream()
                    .map(tripStopMapper::toDto)
                    .collect(Collectors.toList());
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Trip stops fetched successfully!");
            response.setData(stopDTOs);
        } catch (Exception e) {
            log.error("Error fetching stops for trip ID {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while fetching trip stops.");
        }
        return response;
    }

    @Override
    @Transactional
    public GenericDataDTO addStop(Long tripId, TripStopDTO stopDTO) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<Trip> tripOptional = tripRepository.findById(tripId);
            if (tripOptional.isEmpty()) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Trip not found with ID: " + tripId);
                return response;
            }

            TripStop stop = tripStopMapper.toEntity(stopDTO);
            stop.setTrip(tripOptional.get());
            if (stop.getSequenceOrder() == null) {
                long count = tripStopRepository.findByTrip_TripIdOrderBySequenceOrderAsc(tripId).size();
                stop.setSequenceOrder((int) count + 1);
            }

            TripStop savedStop = tripStopRepository.save(stop);
            response.setResponseCode(CommonStatusCode.CREATED);
            response.setResponseMessege("Trip stop added successfully!");
            response.setData(tripStopMapper.toDto(savedStop));
        } catch (Exception e) {
            log.error("Error adding stop to trip ID {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while adding the trip stop.");
            response.setErrors(new Object[]{e.getMessage()});
        }
        return response;
    }

    @Override
    @Transactional
    public GenericDataDTO removeStop(Long tripId, Long stopId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<TripStop> stopOptional = tripStopRepository.findById(stopId);
            if (stopOptional.isEmpty() || stopOptional.get().getTrip() == null ||
                    !tripId.equals(stopOptional.get().getTrip().getTripId())) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Trip stop not found with ID: " + stopId + " for trip: " + tripId);
                return response;
            }
            tripStopRepository.deleteById(stopId);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Trip stop removed successfully!");
        } catch (Exception e) {
            log.error("Error removing stop with ID {}: {}", stopId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while removing the trip stop.");
        }
        return response;
    }

    @Override
    @Transactional
    public GenericDataDTO reorderStops(Long tripId, List<Long> orderedStopIds) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            List<TripStop> stops = tripStopRepository.findByTrip_TripIdOrderBySequenceOrderAsc(tripId);
            for (TripStop stop : stops) {
                int newIndex = orderedStopIds.indexOf(stop.getTripStopId());
                if (newIndex >= 0) {
                    stop.setSequenceOrder(newIndex + 1);
                }
            }
            List<TripStop> savedStops = tripStopRepository.saveAll(stops);
            List<TripStopDTO> stopDTOs = savedStops.stream()
                    .sorted((a, b) -> a.getSequenceOrder().compareTo(b.getSequenceOrder()))
                    .map(tripStopMapper::toDto)
                    .collect(Collectors.toList());

            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Trip stops reordered successfully!");
            response.setData(stopDTOs);
        } catch (Exception e) {
            log.error("Error reordering stops for trip ID {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while reordering trip stops.");
            response.setErrors(new Object[]{e.getMessage()});
        }
        return response;
    }
}
