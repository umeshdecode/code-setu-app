package com.cargosetu.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.customer.entity.Shifting;
import com.cargosetu.dto.TripDTO;
import com.cargosetu.mapper.TripMapper;
import com.cargosetu.model.Driver;
import com.cargosetu.model.Staff;
import com.cargosetu.model.Trip;
import com.cargosetu.model.TripStatus;
import com.cargosetu.model.Vehicle;
import com.cargosetu.notification.event.GenericNotificationEvent;
import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.repo.DriverRepository;
import com.cargosetu.repo.ShiftingRepository;
import com.cargosetu.repo.StaffRepository;
import com.cargosetu.repo.TripRepository;
import com.cargosetu.repo.TripStatusHistoryRepository;
import com.cargosetu.repo.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Covers the "trip assigned" push event (plan's first chosen event, retargeted
 * to company staff - Driver has no login account to push to). Cross-company
 * isolation mirrors plan §13 "Two users, separate tokens": company A's event
 * must never contain company B's staff ids.
 */
@ExtendWith(MockitoExtension.class)
class TripPushNotificationTest {

    @Mock private TripRepository tripRepository;
    @Mock private TripMapper tripMapper;
    @Mock private ShiftingRepository shiftingRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private TripStatusHistoryRepository tripStatusHistoryRepository;
    @Mock private StaffRepository staffRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private TripService tripService;
    private LoggedInUser user;

    @BeforeEach
    void setUp() {
        tripService = new TripService(tripRepository, tripMapper, shiftingRepository, driverRepository,
                vehicleRepository, tripStatusHistoryRepository, staffRepository, eventPublisher);
        user = new LoggedInUser();
        user.setEmail("ops@example.com");
        lenient().when(tripRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(tripMapper.toDto(any())).thenReturn(new TripDTO());
    }

    private Shifting shiftingOf(Integer companyId) {
        Shifting s = new Shifting();
        s.setCompanyId(companyId);
        s.setShiftFromCity("Ahmedabad");
        s.setShiftToCity("Bangalore");
        return s;
    }

    private Driver driver(Long id, String name) {
        Driver d = new Driver();
        d.setId(id);
        d.setName(name);
        return d;
    }

    private Vehicle vehicle(Long id) {
        Vehicle v = new Vehicle();
        v.setId(id);
        return v;
    }

    @Test
    void creatingAnAlreadyAssignedTripNotifiesOnlyItsOwnCompanyStaff() {
        TripDTO dto = new TripDTO();
        dto.setDriverId(1L);
        dto.setVehicleId(1L);
        dto.setStatus(TripStatus.CREATED);

        Trip trip = new Trip();
        trip.setStatus(TripStatus.CREATED);
        trip.setShifting(shiftingOf(7));
        when(tripMapper.toEntity(dto)).thenReturn(trip);
        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver(1L, "Ramesh")));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle(1L)));
        when(staffRepository.findAllByCompanyIdAndIsDeletedFalse(7))
                .thenReturn(List.of(staffWithId(11L), staffWithId(12L)));

        tripService.createTrip(user, dto);

        ArgumentCaptor<GenericNotificationEvent> captor = ArgumentCaptor.forClass(GenericNotificationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        GenericNotificationEvent event = captor.getValue();
        assertThat(event.channel()).isEqualTo(NotificationChannel.PUSH);
        assertThat(event.templateCode()).isEqualTo("TRIP_ASSIGNED_STAFF");
        assertThat(event.recipients()).containsExactlyInAnyOrder("STAFF:11", "STAFF:12");

        verify(staffRepository, never()).findAllByCompanyIdAndIsDeletedFalse(argThat(id -> !id.equals(7)));
    }

    @Test
    void creatingATripWithOnlyADriverDoesNotNotifyYet() {
        TripDTO dto = new TripDTO();
        dto.setDriverId(1L);
        dto.setStatus(TripStatus.CREATED);

        Trip trip = new Trip();
        trip.setStatus(TripStatus.CREATED);
        trip.setShifting(shiftingOf(7));
        when(tripMapper.toEntity(dto)).thenReturn(trip);
        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver(1L, "Ramesh")));

        tripService.createTrip(user, dto);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void updatingAnAlreadyAssignedTripWithNoPairingChangeDoesNotRenotify() {
        Trip existing = new Trip();
        existing.setTripId(5L);
        existing.setStatus(TripStatus.CREATED);
        existing.setDriver(driver(1L, "Ramesh"));
        existing.setVehicle(vehicle(1L));
        existing.setShifting(shiftingOf(7));
        when(tripRepository.findById(5L)).thenReturn(Optional.of(existing));

        TripDTO dto = new TripDTO();
        dto.setRemarks("No change to pairing");
        dto.setStatus(TripStatus.CREATED);

        tripService.updateTrip(user, 5L, dto);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void reassigningAnAlreadyAssignedTripNotifiesAgain() {
        Trip existing = new Trip();
        existing.setTripId(5L);
        existing.setStatus(TripStatus.CREATED);
        existing.setDriver(driver(1L, "Ramesh"));
        existing.setVehicle(vehicle(1L));
        existing.setShifting(shiftingOf(9));
        when(tripRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(driverRepository.findById(2L)).thenReturn(Optional.of(driver(2L, "Suresh")));
        when(staffRepository.findAllByCompanyIdAndIsDeletedFalse(9)).thenReturn(List.of(staffWithId(21L)));

        TripDTO dto = new TripDTO();
        dto.setDriverId(2L);
        dto.setStatus(TripStatus.CREATED);

        tripService.updateTrip(user, 5L, dto);

        ArgumentCaptor<GenericNotificationEvent> captor = ArgumentCaptor.forClass(GenericNotificationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().recipients()).containsExactly("STAFF:21");
    }

    private Staff staffWithId(Long id) {
        Staff s = new Staff();
        s.setStaffId(id);
        return s;
    }
}
