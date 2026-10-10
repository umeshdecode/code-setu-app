package com.cargosetu.marketplace.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.marketplace.dto.LocationUpdateRequest;
import com.cargosetu.marketplace.dto.TripEventRequest;
import com.cargosetu.marketplace.entity.MarketplaceBooking;
import com.cargosetu.marketplace.entity.MarketplaceTrip;
import com.cargosetu.marketplace.entity.MktTripLocationHistory;
import com.cargosetu.marketplace.entity.TripEvent;
import com.cargosetu.marketplace.mapper.MarketplaceTripMapper;
import com.cargosetu.marketplace.mapper.TripEventMapper;
import com.cargosetu.marketplace.repository.MarketplaceTripRepository;
import com.cargosetu.marketplace.repository.MktTripLocationHistoryRepository;
import com.cargosetu.marketplace.repository.TripEventRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import com.cargosetu.marketplace.enums.MarketplaceTripStatus;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketplaceTripService {

    private static final List<MarketplaceTripStatus> ACTIVE_STATUSES = List.of(
            MarketplaceTripStatus.BOOKED,
            MarketplaceTripStatus.DRIVER_ASSIGNED,
            MarketplaceTripStatus.VEHICLE_ASSIGNED,
            MarketplaceTripStatus.DRIVER_ACCEPTED,
            MarketplaceTripStatus.REACHED_LOADING,
            MarketplaceTripStatus.LOADING_STARTED,
            MarketplaceTripStatus.LOADING_COMPLETED,
            MarketplaceTripStatus.IN_TRANSIT,
            MarketplaceTripStatus.REACHED_DESTINATION,
            MarketplaceTripStatus.UNLOADING_STARTED,
            MarketplaceTripStatus.DELAYED);

    private final MarketplaceTripRepository tripRepository;
    private final TripEventRepository eventRepository;
    private final MktTripLocationHistoryRepository locationHistoryRepository;
    private final MarketplaceTripMapper tripMapper;
    private final TripEventMapper eventMapper;
    private final MarketplaceAuditService auditService;

    @Transactional
    public MarketplaceTrip createTripFromBooking(MarketplaceBooking booking) {
        MarketplaceTrip trip = new MarketplaceTrip();
        long count = tripRepository.count() + 1;
        trip.setTripNumber(String.format("MT-%s-%04d", LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")), count));
        trip.setBookingId(booking.getId());
        trip.setVehicleId(booking.getVehicleId());
        trip.setDriverId(booking.getDriverId());
        trip.setCompanyId(booking.getTransporterCompanyId());
        trip.setStatus(MarketplaceTripStatus.BOOKED);
        trip = tripRepository.save(trip);

        TripEvent event = new TripEvent();
        event.setTripId(trip.getId());
        event.setEventType("TRIP_CREATED");
        event.setEventTime(LocalDateTime.now());
        event.setNewStatus(MarketplaceTripStatus.BOOKED.name());
        eventRepository.save(event);

        return trip;
    }

    public GenericDataDTO getTrip(Long tripId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            response.setData(tripMapper.toDto(tripRepository.findById(tripId).orElse(null)));
            response.setResponseCode(CommonStatusCode.SUCCESS);
        } catch (Exception e) {
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    public GenericDataDTO getTripByBooking(Long bookingId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            response.setData(tripMapper.toDto(tripRepository.findByBookingId(bookingId).orElse(null)));
            response.setResponseCode(CommonStatusCode.SUCCESS);
        } catch (Exception e) {
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @Transactional
    public GenericDataDTO addTripEvent(LoggedInUser user, Long tripId, TripEventRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            MarketplaceTrip trip = tripRepository.findById(tripId).orElse(null);
            if (trip != null) {
                MarketplaceTripStatus oldStatus = trip.getStatus();
                if (request.getNewStatus() != null) {
                    trip.setStatus(request.getNewStatus());
                }

                if (MarketplaceTripStatus.IN_TRANSIT == trip.getStatus() && trip.getActualStartTime() == null) {
                    trip.setActualStartTime(LocalDateTime.now());
                } else if (MarketplaceTripStatus.DELIVERED == trip.getStatus()) {
                    trip.setActualEndTime(LocalDateTime.now());
                }

                tripRepository.save(trip);

                String oldName = oldStatus != null ? oldStatus.name() : null;
                String newName = trip.getStatus() != null ? trip.getStatus().name() : null;

                TripEvent event = new TripEvent();
                event.setTripId(trip.getId());
                event.setEventType(request.getEventType());
                event.setEventTime(LocalDateTime.now());
                event.setLocation(request.getLocation());
                event.setLat(request.getLat());
                event.setLng(request.getLng());
                event.setEvidenceFileRef(request.getEvidenceFileRef());
                event.setActorStaffId(user.getStaffId());
                event.setPreviousStatus(oldName);
                event.setNewStatus(newName);
                event.setRemarks(request.getRemarks());
                eventRepository.save(event);

                auditService.logEvent(user.getCompanyId(), user.getStaffId(), "MarketplaceTrip", tripId, "UPDATE_STATUS", oldName, newName);

                response.setResponseCode(CommonStatusCode.SUCCESS);
            }
        } catch (Exception e) {
            log.error("Error adding trip event", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @Transactional
    public GenericDataDTO updateLocation(LoggedInUser user, Long tripId, LocationUpdateRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            MarketplaceTrip trip = tripRepository.findById(tripId).orElse(null);
            if (trip != null) {
                MktTripLocationHistory location = new MktTripLocationHistory();
                location.setTripId(trip.getId());
                location.setVehicleId(trip.getVehicleId());
                location.setLat(request.getLat());
                location.setLng(request.getLng());
                location.setAccuracy(request.getAccuracy());
                location.setSource(request.getSource());
                location.setCapturedAt(LocalDateTime.now());
                locationHistoryRepository.save(location);
                response.setResponseCode(CommonStatusCode.SUCCESS);
            }
        } catch (Exception e) {
            log.error("Error updating trip location", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    public GenericDataDTO getTripTimeline(Long tripId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            response.setDataList(eventMapper.toResponseList(eventRepository.findByTripIdOrderByCreatedAtAsc(tripId)));
            response.setResponseCode(CommonStatusCode.SUCCESS);
        } catch (Exception e) {
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    public GenericDataDTO getActiveTrips(LoggedInUser user) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            response.setDataList(tripRepository.findByCompanyIdAndStatusIn(user.getCompanyId(), ACTIVE_STATUSES).stream().map(tripMapper::toDto).toList());
            response.setResponseCode(CommonStatusCode.SUCCESS);
        } catch (Exception e) {
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }
}
