package com.cargosetu.service.impl;

import com.cargosetu.handler.CustomValidationException;
import com.cargosetu.model.Driver;
import com.cargosetu.model.DriverVehicleAssignment;
import com.cargosetu.model.Vehicle;
import com.cargosetu.repo.DriverRepository;
import com.cargosetu.repo.DriverVehicleAssignmentRepository;
import com.cargosetu.repo.VehicleRepository;
import com.cargosetu.utils.CommonStatusCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverVehicleAssignmentServiceImplTest {

    @Mock
    private DriverVehicleAssignmentRepository repository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private DriverVehicleAssignmentServiceImpl service;

    private Driver driver;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        driver = new Driver();
        driver.setId(1L);
        driver.setStatus("AVAILABLE");

        vehicle = new Vehicle();
        vehicle.setId(2L);
        vehicle.setStatus("AVAILABLE");
    }

    private DriverVehicleAssignment existingAssignment(Long id, Driver d, Vehicle v, boolean active) {
        DriverVehicleAssignment a = new DriverVehicleAssignment();
        a.setId(id);
        a.setDriver(d);
        a.setVehicle(v);
        a.setActive(active);
        return a;
    }

    // ---------- assignDriver ----------

    @Test
    void assignDriver_happyPath_createsAssignmentAndUpdatesStatuses() {
        when(driverRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(vehicle));
        when(repository.findByDriverIdAndActiveTrue(1L)).thenReturn(Collections.emptyList());
        when(repository.findByVehicleIdAndActiveTrue(2L)).thenReturn(Collections.emptyList());
        when(repository.save(any(DriverVehicleAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

        DriverVehicleAssignment result = service.assignDriver(1L, 2L);

        assertThat(result.getActive()).isTrue();
        assertThat(driver.getStatus()).isEqualTo("ASSIGNED");
        assertThat(vehicle.getStatus()).isEqualTo("ON_TRIP");
        verify(driverRepository).save(driver);
        verify(vehicleRepository).save(vehicle);
        verify(repository).save(any(DriverVehicleAssignment.class));
    }

    @Test
    void assignDriver_driverNotFound_throwsNotFound() {
        when(driverRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignDriver(1L, 2L))
                .isInstanceOf(CustomValidationException.class)
                .extracting(ex -> ((CustomValidationException) ex).getErrorCode())
                .isEqualTo(CommonStatusCode.NOT_FOUND);

        verifyNoInteractions(vehicleRepository);
    }

    @Test
    void assignDriver_vehicleNotFound_throwsNotFound() {
        when(driverRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByIdForUpdate(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignDriver(1L, 2L))
                .isInstanceOf(CustomValidationException.class)
                .extracting(ex -> ((CustomValidationException) ex).getErrorCode())
                .isEqualTo(CommonStatusCode.NOT_FOUND);
    }

    @Test
    void assignDriver_driverNotAvailable_throwsConflict() {
        driver.setStatus("ASSIGNED");
        when(driverRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> service.assignDriver(1L, 2L))
                .isInstanceOf(CustomValidationException.class)
                .extracting(ex -> ((CustomValidationException) ex).getErrorCode())
                .isEqualTo(CommonStatusCode.CONFLICT);

        verifyNoInteractions(repository);
    }

    @Test
    void assignDriver_vehicleNotAvailable_throwsConflict() {
        vehicle.setStatus("MAINTENANCE");
        when(driverRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> service.assignDriver(1L, 2L))
                .isInstanceOf(CustomValidationException.class)
                .extracting(ex -> ((CustomValidationException) ex).getErrorCode())
                .isEqualTo(CommonStatusCode.CONFLICT);
    }

    @Test
    void assignDriver_driverAlreadyActivelyAssigned_throwsConflict_doesNotReassign() {
        DriverVehicleAssignment existing = existingAssignment(99L, driver, new Vehicle(), true);
        when(driverRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(vehicle));
        when(repository.findByDriverIdAndActiveTrue(1L)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.assignDriver(1L, 2L))
                .isInstanceOf(CustomValidationException.class)
                .extracting(ex -> ((CustomValidationException) ex).getErrorCode())
                .isEqualTo(CommonStatusCode.CONFLICT);

        assertThat(existing.getActive()).isTrue();
        verify(repository, never()).save(any(DriverVehicleAssignment.class));
    }

    @Test
    void assignDriver_vehicleAlreadyActivelyAssigned_throwsConflict() {
        DriverVehicleAssignment existing = existingAssignment(99L, new Driver(), vehicle, true);
        when(driverRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(vehicle));
        when(repository.findByDriverIdAndActiveTrue(1L)).thenReturn(Collections.emptyList());
        when(repository.findByVehicleIdAndActiveTrue(2L)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.assignDriver(1L, 2L))
                .isInstanceOf(CustomValidationException.class)
                .extracting(ex -> ((CustomValidationException) ex).getErrorCode())
                .isEqualTo(CommonStatusCode.CONFLICT);

        verify(repository, never()).save(any(DriverVehicleAssignment.class));
    }

    @Test
    void assignDriver_dbConstraintViolation_translatedToConflict() {
        when(driverRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(vehicle));
        when(repository.findByDriverIdAndActiveTrue(1L)).thenReturn(Collections.emptyList());
        when(repository.findByVehicleIdAndActiveTrue(2L)).thenReturn(Collections.emptyList());
        when(repository.save(any(DriverVehicleAssignment.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> service.assignDriver(1L, 2L))
                .isInstanceOf(CustomValidationException.class)
                .extracting(ex -> ((CustomValidationException) ex).getErrorCode())
                .isEqualTo(CommonStatusCode.CONFLICT);
    }

    // ---------- unassign ----------

    @Test
    void unassign_happyPath_bothFlipToAvailable_andRemarksSet() {
        driver.setStatus("ASSIGNED");
        vehicle.setStatus("ON_TRIP");
        DriverVehicleAssignment assignment = existingAssignment(10L, driver, vehicle, true);

        when(repository.findByIdForUpdate(10L)).thenReturn(Optional.of(assignment));
        when(driverRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(vehicle));
        when(repository.findByDriverIdAndActiveTrueExcludingId(1L, 10L)).thenReturn(Collections.emptyList());
        when(repository.findByVehicleIdAndActiveTrueExcludingId(2L, 10L)).thenReturn(Collections.emptyList());

        service.unassign(10L, "returned to depot");

        assertThat(assignment.getActive()).isFalse();
        assertThat(assignment.getRemarks()).isEqualTo("returned to depot");
        assertThat(driver.getStatus()).isEqualTo("AVAILABLE");
        assertThat(vehicle.getStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    void unassign_reasonNull_doesNotOverwriteRemarks() {
        driver.setStatus("ASSIGNED");
        vehicle.setStatus("ON_TRIP");
        DriverVehicleAssignment assignment = existingAssignment(10L, driver, vehicle, true);
        assignment.setRemarks("pre-existing note");

        when(repository.findByIdForUpdate(10L)).thenReturn(Optional.of(assignment));
        when(driverRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(vehicle));
        when(repository.findByDriverIdAndActiveTrueExcludingId(1L, 10L)).thenReturn(Collections.emptyList());
        when(repository.findByVehicleIdAndActiveTrueExcludingId(2L, 10L)).thenReturn(Collections.emptyList());

        service.unassign(10L, null);

        assertThat(assignment.getRemarks()).isEqualTo("pre-existing note");
    }

    @Test
    void unassign_assignmentNotFound_throwsNotFound() {
        when(repository.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.unassign(10L, "x"))
                .isInstanceOf(CustomValidationException.class)
                .extracting(ex -> ((CustomValidationException) ex).getErrorCode())
                .isEqualTo(CommonStatusCode.NOT_FOUND);
    }

    @Test
    void unassign_alreadyInactive_throwsConflict() {
        DriverVehicleAssignment assignment = existingAssignment(10L, driver, vehicle, false);
        when(repository.findByIdForUpdate(10L)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> service.unassign(10L, "x"))
                .isInstanceOf(CustomValidationException.class)
                .extracting(ex -> ((CustomValidationException) ex).getErrorCode())
                .isEqualTo(CommonStatusCode.CONFLICT);

        verifyNoInteractions(driverRepository);
    }

    @Test
    void unassign_driverHasOtherActiveAssignment_statusNotFlippedToAvailable() {
        driver.setStatus("ASSIGNED");
        vehicle.setStatus("ON_TRIP");
        DriverVehicleAssignment assignment = existingAssignment(10L, driver, vehicle, true);
        DriverVehicleAssignment otherActive = existingAssignment(11L, driver, new Vehicle(), true);

        when(repository.findByIdForUpdate(10L)).thenReturn(Optional.of(assignment));
        when(driverRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(vehicle));
        when(repository.findByDriverIdAndActiveTrueExcludingId(1L, 10L)).thenReturn(List.of(otherActive));
        when(repository.findByVehicleIdAndActiveTrueExcludingId(2L, 10L)).thenReturn(Collections.emptyList());

        service.unassign(10L, null);

        assertThat(driver.getStatus()).isEqualTo("ASSIGNED");
        assertThat(vehicle.getStatus()).isEqualTo("AVAILABLE");
        verify(driverRepository, never()).save(driver);
    }

    @Test
    void unassign_vehicleHasOtherActiveAssignment_statusNotFlippedToAvailable() {
        driver.setStatus("ASSIGNED");
        vehicle.setStatus("ON_TRIP");
        DriverVehicleAssignment assignment = existingAssignment(10L, driver, vehicle, true);
        DriverVehicleAssignment otherActive = existingAssignment(12L, new Driver(), vehicle, true);

        when(repository.findByIdForUpdate(10L)).thenReturn(Optional.of(assignment));
        when(driverRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(vehicle));
        when(repository.findByDriverIdAndActiveTrueExcludingId(1L, 10L)).thenReturn(Collections.emptyList());
        when(repository.findByVehicleIdAndActiveTrueExcludingId(2L, 10L)).thenReturn(List.of(otherActive));

        service.unassign(10L, null);

        assertThat(vehicle.getStatus()).isEqualTo("ON_TRIP");
        assertThat(driver.getStatus()).isEqualTo("AVAILABLE");
        verify(vehicleRepository, never()).save(vehicle);
    }

    @Test
    void unassign_driverStatusIsInactiveNotAssigned_notOverwritten() {
        driver.setStatus("INACTIVE");
        vehicle.setStatus("ON_TRIP");
        DriverVehicleAssignment assignment = existingAssignment(10L, driver, vehicle, true);

        when(repository.findByIdForUpdate(10L)).thenReturn(Optional.of(assignment));
        when(driverRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(vehicle));
        when(repository.findByDriverIdAndActiveTrueExcludingId(1L, 10L)).thenReturn(Collections.emptyList());
        when(repository.findByVehicleIdAndActiveTrueExcludingId(2L, 10L)).thenReturn(Collections.emptyList());

        service.unassign(10L, null);

        assertThat(driver.getStatus()).isEqualTo("INACTIVE");
        verify(driverRepository, never()).save(driver);
    }

    @Test
    void unassign_vehicleStatusIsMaintenanceNotOnTrip_notOverwritten() {
        driver.setStatus("ASSIGNED");
        vehicle.setStatus("MAINTENANCE");
        DriverVehicleAssignment assignment = existingAssignment(10L, driver, vehicle, true);

        when(repository.findByIdForUpdate(10L)).thenReturn(Optional.of(assignment));
        when(driverRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(vehicle));
        when(repository.findByDriverIdAndActiveTrueExcludingId(1L, 10L)).thenReturn(Collections.emptyList());
        when(repository.findByVehicleIdAndActiveTrueExcludingId(2L, 10L)).thenReturn(Collections.emptyList());

        service.unassign(10L, null);

        assertThat(vehicle.getStatus()).isEqualTo("MAINTENANCE");
        verify(vehicleRepository, never()).save(vehicle);
    }
}
