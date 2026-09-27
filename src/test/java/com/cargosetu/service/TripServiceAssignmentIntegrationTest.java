package com.cargosetu.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.dto.TripDTO;
import com.cargosetu.handler.CustomValidationException;
import com.cargosetu.mapper.TripMapper;
import com.cargosetu.model.Driver;
import com.cargosetu.model.DriverVehicleAssignment;
import com.cargosetu.model.Trip;
import com.cargosetu.model.TripStatus;
import com.cargosetu.model.Vehicle;
import com.cargosetu.repo.DriverRepository;
import com.cargosetu.repo.DriverVehicleAssignmentRepository;
import com.cargosetu.repo.ShiftingRepository;
import com.cargosetu.repo.TripRepository;
import com.cargosetu.repo.TripStatusHistoryRepository;
import com.cargosetu.repo.VehicleRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Covers the fix wiring Trip creation/update to DriverVehicleAssignmentService,
 * so assigning a driver+vehicle to a trip (and completing/cancelling/closing a
 * trip) keeps Driver.status / Vehicle.status / driver_vehicle_assignments in
 * sync with the trip lifecycle, instead of bypassing that entirely.
 */
@ExtendWith(MockitoExtension.class)
class TripServiceAssignmentIntegrationTest {

    @Mock
    private TripRepository tripRepository;
    @Mock
    private TripMapper tripMapper;
    @Mock
    private ShiftingRepository shiftingRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private TripStatusHistoryRepository tripStatusHistoryRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private DriverVehicleAssignmentService driverVehicleAssignmentService;
    @Mock
    private DriverVehicleAssignmentRepository driverVehicleAssignmentRepository;

    private TripService tripService;
    private LoggedInUser user;

    @BeforeEach
    void setUp() {
        tripService = new TripService(tripRepository, tripMapper, shiftingRepository, driverRepository,
                vehicleRepository, tripStatusHistoryRepository, eventPublisher,
                driverVehicleAssignmentService, driverVehicleAssignmentRepository, new TripAccessPolicy());

        user = mock(LoggedInUser.class);
        lenient().when(user.getUsername()).thenReturn("tester");
    }

    private Driver driverWithId(Long id) {
        Driver d = new Driver();
        d.setId(id);
        return d;
    }

    private Vehicle vehicleWithId(Long id) {
        Vehicle v = new Vehicle();
        v.setId(id);
        return v;
    }

    private Trip newTripStub() {
        Trip trip = new Trip();
        trip.setTripId(100L);
        trip.setStatus(TripStatus.CREATED);
        return trip;
    }

    // ---------- createTrip ----------

    @Test
    void createTrip_withDriverAndVehicle_callsAssignDriver() {
        TripDTO dto = new TripDTO();
        dto.setDriverId(1L);
        dto.setVehicleId(2L);
        dto.setStatus(TripStatus.CREATED);

        Trip trip = newTripStub();
        when(tripMapper.toEntity(dto)).thenReturn(trip);
        when(driverRepository.findById(1L)).thenReturn(Optional.of(driverWithId(1L)));
        when(vehicleRepository.findById(2L)).thenReturn(Optional.of(vehicleWithId(2L)));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tripMapper.toDto(any(Trip.class))).thenReturn(dto);

        GenericDataDTO response = tripService.createTrip(user, dto);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.CREATED);
        verify(driverVehicleAssignmentService).assignDriver(1L, 2L);
        verify(tripRepository).save(any(Trip.class));
    }

    @Test
    void createTrip_withOnlyDriverId_doesNotCallAssignDriver() {
        TripDTO dto = new TripDTO();
        dto.setDriverId(1L);
        dto.setStatus(TripStatus.CREATED);

        Trip trip = newTripStub();
        when(tripMapper.toEntity(dto)).thenReturn(trip);
        when(driverRepository.findById(1L)).thenReturn(Optional.of(driverWithId(1L)));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tripMapper.toDto(any(Trip.class))).thenReturn(dto);

        GenericDataDTO response = tripService.createTrip(user, dto);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.CREATED);
        verifyNoInteractions(driverVehicleAssignmentService);
    }

    @Test
    void createTrip_assignDriverRejects_tripNotCreated_errorPropagated() {
        TripDTO dto = new TripDTO();
        dto.setDriverId(1L);
        dto.setVehicleId(2L);
        dto.setStatus(TripStatus.CREATED);

        Trip trip = newTripStub();
        when(tripMapper.toEntity(dto)).thenReturn(trip);
        when(driverRepository.findById(1L)).thenReturn(Optional.of(driverWithId(1L)));
        when(vehicleRepository.findById(2L)).thenReturn(Optional.of(vehicleWithId(2L)));
        doThrow(new CustomValidationException(CommonStatusCode.CONFLICT, "Driver 1 is not AVAILABLE"))
                .when(driverVehicleAssignmentService).assignDriver(1L, 2L);

        GenericDataDTO response = tripService.createTrip(user, dto);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.CONFLICT);
        assertThat(response.getResponseMessege()).isEqualTo("Driver 1 is not AVAILABLE");
        verify(tripRepository, never()).save(any(Trip.class));
    }

    // ---------- updateTrip ----------

    @Test
    void updateTrip_newPairing_callsAssignDriver_noRelease() {
        Trip existing = newTripStub();
        existing.setStatus(TripStatus.CREATED);
        when(tripRepository.findById(100L)).thenReturn(Optional.of(existing));

        TripDTO dto = new TripDTO();
        dto.setDriverId(1L);
        dto.setVehicleId(2L);
        dto.setStatus(TripStatus.CREATED);

        when(driverRepository.findById(1L)).thenReturn(Optional.of(driverWithId(1L)));
        when(vehicleRepository.findById(2L)).thenReturn(Optional.of(vehicleWithId(2L)));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tripMapper.toDto(any(Trip.class))).thenReturn(dto);

        GenericDataDTO response = tripService.updateTrip(user, 100L, dto);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.SUCCESS);
        verify(driverVehicleAssignmentService).assignDriver(1L, 2L);
        verifyNoInteractions(driverVehicleAssignmentRepository);
    }

    @Test
    void updateTrip_reassignment_releasesOldPairing_thenAssignsNew() {
        Trip existing = newTripStub();
        existing.setStatus(TripStatus.CREATED);
        existing.setDriver(driverWithId(1L));
        existing.setVehicle(vehicleWithId(2L));
        when(tripRepository.findById(100L)).thenReturn(Optional.of(existing));

        DriverVehicleAssignment oldAssignment = new DriverVehicleAssignment();
        oldAssignment.setId(50L);
        when(driverVehicleAssignmentRepository.findByDriverIdAndActiveTrue(1L))
                .thenReturn(List.of(oldAssignment));

        TripDTO dto = new TripDTO();
        dto.setDriverId(3L);
        dto.setVehicleId(4L);
        dto.setStatus(TripStatus.CREATED);

        when(driverRepository.findById(3L)).thenReturn(Optional.of(driverWithId(3L)));
        when(vehicleRepository.findById(4L)).thenReturn(Optional.of(vehicleWithId(4L)));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tripMapper.toDto(any(Trip.class))).thenReturn(dto);

        GenericDataDTO response = tripService.updateTrip(user, 100L, dto);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.SUCCESS);
        verify(driverVehicleAssignmentService).unassign(eq(50L), contains("Reassigned from trip"));
        verify(driverVehicleAssignmentService).assignDriver(3L, 4L);
    }

    @Test
    void updateTrip_tripFinished_releasesAssignment_doesNotReassign() {
        Trip existing = newTripStub();
        existing.setStatus(TripStatus.STARTED);
        existing.setDriver(driverWithId(1L));
        existing.setVehicle(vehicleWithId(2L));
        when(tripRepository.findById(100L)).thenReturn(Optional.of(existing));

        DriverVehicleAssignment activeAssignment = new DriverVehicleAssignment();
        activeAssignment.setId(50L);
        when(driverVehicleAssignmentRepository.findByDriverIdAndActiveTrue(1L))
                .thenReturn(List.of(activeAssignment));

        TripDTO dto = new TripDTO();
        // Frontend resends the same driver/vehicle alongside the completion status.
        dto.setDriverId(1L);
        dto.setVehicleId(2L);
        dto.setStatus(TripStatus.COMPLETED);

        when(driverRepository.findById(1L)).thenReturn(Optional.of(driverWithId(1L)));
        when(vehicleRepository.findById(2L)).thenReturn(Optional.of(vehicleWithId(2L)));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tripMapper.toDto(any(Trip.class))).thenReturn(dto);

        GenericDataDTO response = tripService.updateTrip(user, 100L, dto);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.SUCCESS);
        verify(driverVehicleAssignmentService).unassign(eq(50L), contains("COMPLETED"));
        verify(driverVehicleAssignmentService, never()).assignDriver(any(), any());
    }

    @Test
    void updateTrip_noDriverVehicleChange_noStatusFinish_doesNothingWithAssignments() {
        Trip existing = newTripStub();
        existing.setStatus(TripStatus.CREATED);
        existing.setDriver(driverWithId(1L));
        existing.setVehicle(vehicleWithId(2L));
        when(tripRepository.findById(100L)).thenReturn(Optional.of(existing));

        TripDTO dto = new TripDTO();
        dto.setDriverId(1L);
        dto.setVehicleId(2L);
        dto.setStatus(TripStatus.CREATED);
        dto.setRemarks("just a note");

        when(driverRepository.findById(1L)).thenReturn(Optional.of(driverWithId(1L)));
        when(vehicleRepository.findById(2L)).thenReturn(Optional.of(vehicleWithId(2L)));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tripMapper.toDto(any(Trip.class))).thenReturn(dto);

        GenericDataDTO response = tripService.updateTrip(user, 100L, dto);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.SUCCESS);
        verifyNoInteractions(driverVehicleAssignmentService);
        verifyNoInteractions(driverVehicleAssignmentRepository);
    }

    @Test
    void updateTrip_releaseFindsNoActiveAssignment_doesNotFailTrip() {
        Trip existing = newTripStub();
        existing.setStatus(TripStatus.STARTED);
        existing.setDriver(driverWithId(1L));
        existing.setVehicle(vehicleWithId(2L));
        when(tripRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(driverVehicleAssignmentRepository.findByDriverIdAndActiveTrue(1L))
                .thenReturn(Collections.emptyList());

        TripDTO dto = new TripDTO();
        dto.setDriverId(1L);
        dto.setVehicleId(2L);
        dto.setStatus(TripStatus.CANCELLED);

        when(driverRepository.findById(1L)).thenReturn(Optional.of(driverWithId(1L)));
        when(vehicleRepository.findById(2L)).thenReturn(Optional.of(vehicleWithId(2L)));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tripMapper.toDto(any(Trip.class))).thenReturn(dto);

        GenericDataDTO response = tripService.updateTrip(user, 100L, dto);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.SUCCESS);
        verifyNoInteractions(driverVehicleAssignmentService);
    }

    @Test
    void updateTrip_newPairingRejected_tripNotSaved() {
        Trip existing = newTripStub();
        existing.setStatus(TripStatus.CREATED);
        when(tripRepository.findById(100L)).thenReturn(Optional.of(existing));

        TripDTO dto = new TripDTO();
        dto.setDriverId(1L);
        dto.setVehicleId(2L);

        when(driverRepository.findById(1L)).thenReturn(Optional.of(driverWithId(1L)));
        when(vehicleRepository.findById(2L)).thenReturn(Optional.of(vehicleWithId(2L)));
        doThrow(new CustomValidationException(CommonStatusCode.CONFLICT, "Vehicle 2 is not AVAILABLE"))
                .when(driverVehicleAssignmentService).assignDriver(1L, 2L);

        GenericDataDTO response = tripService.updateTrip(user, 100L, dto);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.CONFLICT);
        assertThat(response.getResponseMessege()).isEqualTo("Vehicle 2 is not AVAILABLE");
        verify(tripRepository, never()).save(any(Trip.class));
    }
}
