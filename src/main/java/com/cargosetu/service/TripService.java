package com.cargosetu.service;

import com.cargosetu.dto.TripDTO;
import com.cargosetu.mapper.TripMapper;
import com.cargosetu.model.Driver;
import com.cargosetu.model.Trip;
import com.cargosetu.model.TripStatus;
import com.cargosetu.model.Vehicle;
import com.cargosetu.repo.DriverRepository;
import com.cargosetu.repo.ShiftingRepository;
import com.cargosetu.repo.TripRepository;
import com.cargosetu.repo.VehicleRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class TripService {

    private final TripRepository tripRepository;
    private final TripMapper tripMapper;
    private final ShiftingRepository shiftingRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;

    public TripService(TripRepository tripRepository,
                       TripMapper tripMapper,
                       ShiftingRepository shiftingRepository,
                       DriverRepository driverRepository,
                       VehicleRepository vehicleRepository) {
        this.tripRepository = tripRepository;
        this.tripMapper = tripMapper;
        this.shiftingRepository = shiftingRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional
    public GenericDataDTO createTrip(TripDTO tripDTO) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Trip trip = tripMapper.toEntity(tripDTO);

            if (tripDTO.getShiftingId() != null) {
                shiftingRepository.findById(tripDTO.getShiftingId())
                        .ifPresent(trip::setShifting);
            }
            if (tripDTO.getDriverId() != null) {
                driverRepository.findById(tripDTO.getDriverId())
                        .ifPresent(trip::setDriver);
            }
            if (tripDTO.getVehicleId() != null) {
                vehicleRepository.findById(tripDTO.getVehicleId())
                        .ifPresent(trip::setVehicle);
            }

            trip.setCreatedAt(LocalDateTime.now());
            trip.setUpdatedAt(LocalDateTime.now());
            if (trip.getActive() == null) {
                trip.setActive(true);
            }

            Trip savedTrip = tripRepository.save(trip);
            response.setResponseCode(CommonStatusCode.CREATED);
            response.setResponseMessege("Trip created successfully!");
            response.setData(tripMapper.toDto(savedTrip));

        } catch (Exception e) {
            log.error("Error creating trip: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while creating the trip.");
            response.setErrors(new Object[]{e.getMessage()});
        }
        return response;
    }

    public GenericDataDTO getTrip(Long tripId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<Trip> tripOptional = tripRepository.findById(tripId);
            if (tripOptional.isPresent()) {
                response.setResponseCode(CommonStatusCode.SUCCESS);
                response.setResponseMessege("Trip found successfully!");
                response.setData(tripMapper.toDto(tripOptional.get()));
            } else {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Trip not found with ID: " + tripId);
            }
        } catch (Exception e) {
            log.error("Error getting trip with ID {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while retrieving the trip.");
        }
        return response;
    }

    public GenericDataDTO getAllTrips() {
        GenericDataDTO response = new GenericDataDTO();
        try {
            List<Trip> trips = tripRepository.findAll();
            List<TripDTO> tripDTOs = trips.stream()
                    .map(tripMapper::toDto)
                    .toList();
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Trips fetched successfully!");
            response.setData(tripDTOs);
        } catch (Exception e) {
            log.error("Error fetching all trips: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while fetching trips.");
        }
        return response;
    }

    @Transactional
    public GenericDataDTO updateTrip(Long tripId, TripDTO tripDTO) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<Trip> existingTripOptional = tripRepository.findById(tripId);
            if (existingTripOptional.isEmpty()) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Trip not found with ID: " + tripId);
                return response;
            }

            Trip existingTrip = existingTripOptional.get();

            if (tripDTO.getTripNumber() != null) {
                existingTrip.setTripNumber(tripDTO.getTripNumber());
            }
            if (tripDTO.getPlannedStartTime() != null) {
                existingTrip.setPlannedStartTime(tripDTO.getPlannedStartTime());
            }
            if (tripDTO.getActualStartTime() != null) {
                existingTrip.setActualStartTime(tripDTO.getActualStartTime());
            }
            if (tripDTO.getActualEndTime() != null) {
                existingTrip.setActualEndTime(tripDTO.getActualEndTime());
            }
            if (tripDTO.getPlannedDistanceKm() != null) {
                existingTrip.setPlannedDistanceKm(tripDTO.getPlannedDistanceKm());
            }
            if (tripDTO.getActualDistanceKm() != null) {
                existingTrip.setActualDistanceKm(tripDTO.getActualDistanceKm());
            }
            if (tripDTO.getStatus() != null) {
                existingTrip.setStatus(tripDTO.getStatus());
            }
            if (tripDTO.getRemarks() != null) {
                existingTrip.setRemarks(tripDTO.getRemarks());
            }
            if (tripDTO.getActive() != null) {
                existingTrip.setActive(tripDTO.getActive());
            }
            if (tripDTO.getStatus() != null && tripDTO.getStatus().equals(TripStatus.STARTED) ) {
                existingTrip.setActualStartTime(LocalDateTime.now());
            }
            if (tripDTO.getStatus() != null && tripDTO.getStatus().equals(TripStatus.COMPLETED) ) {
                existingTrip.setActualEndTime(LocalDateTime.now());
            }

            if (tripDTO.getShiftingId() != null) {
                shiftingRepository.findById(tripDTO.getShiftingId())
                        .ifPresent(existingTrip::setShifting);
            }
            if (tripDTO.getDriverId() != null) {
                driverRepository.findById(tripDTO.getDriverId())
                        .ifPresent(existingTrip::setDriver);
            }
            if (tripDTO.getVehicleId() != null) {
                vehicleRepository.findById(tripDTO.getVehicleId())
                        .ifPresent(existingTrip::setVehicle);
            }

            existingTrip.setUpdatedAt(LocalDateTime.now());

            Trip savedTrip = tripRepository.save(existingTrip);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Trip updated successfully!");
            response.setData(tripMapper.toDto(savedTrip));

        } catch (Exception e) {
            log.error("Error updating trip with ID {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while updating the trip.");
            response.setErrors(new Object[]{e.getMessage()});
        }
        return response;
    }

    @Transactional
    public GenericDataDTO deleteTrip(Long tripId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<Trip> tripOptional = tripRepository.findById(tripId);
            if (tripOptional.isPresent()) {
                Trip trip = tripOptional.get();
                trip.setActive(false);
                trip.setUpdatedAt(LocalDateTime.now());
                tripRepository.save(trip);
                response.setResponseCode(CommonStatusCode.SUCCESS);
                response.setResponseMessege("Trip deleted successfully!");
            } else {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Trip not found with ID: " + tripId);
            }
        } catch (Exception e) {
            log.error("Error deleting trip with ID {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while deleting the trip.");
        }
        return response;
    }
}
