package com.cargosetu.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.dto.TripDTO;
import com.cargosetu.handler.CustomValidationException;
import com.cargosetu.mapper.TripMapper;
import com.cargosetu.model.*;
import com.cargosetu.notification.event.TripOpenedEvent;
import com.cargosetu.repo.*;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class TripService {

    private final TripRepository tripRepository;
    private final TripMapper tripMapper;
    private final ShiftingRepository shiftingRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final TripStatusHistoryRepository tripStatusHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final DriverVehicleAssignmentService driverVehicleAssignmentService;
    private final DriverVehicleAssignmentRepository driverVehicleAssignmentRepository;
    private final TripAccessPolicy tripAccessPolicy;
    public TripService(TripRepository tripRepository,
                       TripMapper tripMapper,
                       ShiftingRepository shiftingRepository,
                       DriverRepository driverRepository,
                       VehicleRepository vehicleRepository,
                       TripStatusHistoryRepository tripStatusHistoryRepository,
                       ApplicationEventPublisher eventPublisher,
                       DriverVehicleAssignmentService driverVehicleAssignmentService,
                       DriverVehicleAssignmentRepository driverVehicleAssignmentRepository,
                       TripAccessPolicy tripAccessPolicy) {
        this.tripRepository = tripRepository;
        this.tripMapper = tripMapper;
        this.shiftingRepository = shiftingRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.tripStatusHistoryRepository = tripStatusHistoryRepository;
        this.eventPublisher = eventPublisher;
        this.driverVehicleAssignmentService = driverVehicleAssignmentService;
        this.driverVehicleAssignmentRepository = driverVehicleAssignmentRepository;
        this.tripAccessPolicy = tripAccessPolicy;
    }

    @Transactional
    public GenericDataDTO createTrip(LoggedInUser user, TripDTO tripDTO) {
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

            // Assigning both a driver and a vehicle to a trip must go through the same
            // availability/duplicate checks as the standalone assignment API, and keep
            // Driver.status / Vehicle.status / driver_vehicle_assignments in sync.
            if (tripDTO.getDriverId() != null && tripDTO.getVehicleId() != null) {
                driverVehicleAssignmentService.assignDriver(tripDTO.getDriverId(), tripDTO.getVehicleId());
            }

            // Company ownership: the creator's company, else the linked shifting's company.
            Integer companyId = user != null ? user.getCompanyId() : null;
            if (companyId == null && trip.getShifting() != null) {
                companyId = trip.getShifting().getCompanyId();
            }
            trip.setCompanyId(companyId);

            trip.setCreatedAt(LocalDateTime.now());
            trip.setUpdatedAt(LocalDateTime.now());
            if (trip.getActive() == null) {
                trip.setActive(true);
            }

            Trip savedTrip = tripRepository.save(trip);
            TripStatusHistory tripStatusHistory = new TripStatusHistory();
            tripStatusHistory.setOldStatus(savedTrip.getStatus().toString());
            tripStatusHistory.setNewStatus(savedTrip.getStatus().toString());
            tripStatusHistory.setTrip(savedTrip);
            tripStatusHistory.setChangedBy(user.getUsername());
            tripStatusHistory.setChangedAt(LocalDateTime.now());
            tripStatusHistoryRepository.save(tripStatusHistory);
            response.setResponseCode(CommonStatusCode.CREATED);
            response.setResponseMessege("Trip created successfully!");
            response.setData(tripMapper.toDto(savedTrip));

            eventPublisher.publishEvent(new TripOpenedEvent(savedTrip.getTripId()));

        } catch (CustomValidationException e) {
            log.warn("Trip creation rejected: {}", e.getMessage());
            response.setResponseCode(e.getErrorCode());
            response.setResponseMessege(e.getMessage());
        } catch (Exception e) {
            log.error("Error creating trip: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while creating the trip.");
            response.setErrors(new Object[]{e.getMessage()});
        }
        return response;
    }

    public GenericDataDTO getTrip(LoggedInUser user, Long tripId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<Trip> tripOptional = tripRepository.findById(tripId)
                    .filter(trip -> tripAccessPolicy.canAccess(user, trip));
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

    public GenericDataDTO getAllTrips(LoggedInUser user) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            List<Trip> trips = tripRepository.findAll();
            List<TripDTO> tripDTOs = trips.stream()
                    .filter(trip -> tripAccessPolicy.canAccess(user, trip))
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
    public GenericDataDTO updateTrip(LoggedInUser user, Long tripId, TripDTO tripDTO) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<Trip> existingTripOptional = tripRepository.findById(tripId)
                    .filter(trip -> tripAccessPolicy.canAccess(user, trip));
            if (existingTripOptional.isEmpty()) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Trip not found with ID: " + tripId);
                return response;
            }

            Trip existingTrip = existingTripOptional.get();

            Long oldDriverId = existingTrip.getDriver() != null ? existingTrip.getDriver().getId() : null;
            Long oldVehicleId = existingTrip.getVehicle() != null ? existingTrip.getVehicle().getId() : null;

            if (tripDTO.getTripNumber() != null) {
                existingTrip.setTripNumber(tripDTO.getTripNumber());
            }
            if (tripDTO.getPlannedStartTime() != null) {
                existingTrip.setPlannedStartTime(tripDTO.getPlannedStartTime());
            }
            if (tripDTO.getPlannedEndTime() != null) {
                existingTrip.setPlannedEenTime(tripDTO.getPlannedEndTime());
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

            Long newDriverId = existingTrip.getDriver() != null ? existingTrip.getDriver().getId() : null;
            Long newVehicleId = existingTrip.getVehicle() != null ? existingTrip.getVehicle().getId() : null;
            boolean pairingChanged = !Objects.equals(oldDriverId, newDriverId) || !Objects.equals(oldVehicleId, newVehicleId);
            boolean tripFinished = tripDTO.getStatus() != null
                    && (tripDTO.getStatus() == TripStatus.COMPLETED
                        || tripDTO.getStatus() == TripStatus.CANCELLED
                        || tripDTO.getStatus() == TripStatus.CLOSED);

            // Release the previous driver/vehicle's active assignment (back to AVAILABLE)
            // whenever the pairing is changing, or the trip is finishing.
            if ((pairingChanged || tripFinished) && oldDriverId != null && oldVehicleId != null) {
                releaseActiveAssignmentForDriver(oldDriverId,
                        tripFinished ? "Trip " + tripId + " " + tripDTO.getStatus() : "Reassigned from trip " + tripId);
            }

            // A genuinely new pairing (not part of a completion/cancellation call) must go
            // through the same availability/duplicate checks as trip creation.
            if (pairingChanged && !tripFinished && newDriverId != null && newVehicleId != null) {
                driverVehicleAssignmentService.assignDriver(newDriverId, newVehicleId);
            }

            existingTrip.setUpdatedAt(LocalDateTime.now());

            if(!Objects.equals(existingTrip.getStatus(),tripDTO.getStatus())) {
                TripStatusHistory tripStatusHistory = new TripStatusHistory();
                tripStatusHistory.setOldStatus(existingTrip.getStatus().toString());
                tripStatusHistory.setNewStatus(tripDTO.getStatus().toString());
                tripStatusHistory.setTrip(existingTrip);
                tripStatusHistory.setChangedBy(user.getUsername());
                tripStatusHistory.setChangedAt(LocalDateTime.now());
                tripStatusHistoryRepository.save(tripStatusHistory);
            }

            Trip savedTrip = tripRepository.save(existingTrip);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Trip updated successfully!");
            response.setData(tripMapper.toDto(savedTrip));

        } catch (CustomValidationException e) {
            log.warn("Trip update rejected for ID {}: {}", tripId, e.getMessage());
            response.setResponseCode(e.getErrorCode());
            response.setResponseMessege(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating trip with ID {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while updating the trip.");
            response.setErrors(new Object[]{e.getMessage()});
        }
        return response;
    }

    /**
     * Releases the given driver's currently active assignment (if any) back to
     * AVAILABLE, e.g. when a trip is reassigned to a different driver/vehicle or
     * finishes (COMPLETED/CANCELLED/CLOSED). Lenient by design: a trip's own
     * lifecycle must not fail just because the assignment bookkeeping was already
     * reconciled by some other path (e.g. a direct call to the assignments API).
     */
    private void releaseActiveAssignmentForDriver(Long driverId, String reason) {
        List<DriverVehicleAssignment> active = driverVehicleAssignmentRepository.findByDriverIdAndActiveTrue(driverId);
        if (active.isEmpty()) {
            log.warn("No active driver-vehicle assignment found for driver {} while releasing from a trip; skipping.", driverId);
            return;
        }
        for (DriverVehicleAssignment assignment : active) {
            try {
                driverVehicleAssignmentService.unassign(assignment.getId(), reason);
            } catch (CustomValidationException e) {
                log.warn("Could not release assignment {} for driver {}: {}", assignment.getId(), driverId, e.getMessage());
            }
        }
    }

    @Transactional
    public GenericDataDTO deleteTrip(LoggedInUser user, Long tripId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<Trip> tripOptional = tripRepository.findById(tripId)
                    .filter(trip -> tripAccessPolicy.canAccess(user, trip));
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
