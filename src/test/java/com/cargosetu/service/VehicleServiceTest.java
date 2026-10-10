package com.cargosetu.service;

import com.cargosetu.dto.TruckOptionsDto;
import com.cargosetu.dto.VehicleDto;
import com.cargosetu.model.Vehicle;
import com.cargosetu.repo.VehicleRepository;
import com.cargosetu.service.impl.VehicleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private TruckOptionService truckOptionService;

    @InjectMocks
    private VehicleServiceImpl vehicleService;

    private VehicleDto sampleDto;

    @BeforeEach
    void setUp() {
        sampleDto = new VehicleDto();
        sampleDto.setRegistrationNumber("MH12AB1234");
        sampleDto.setChassisNumber("CHASSIS12345");
        sampleDto.setEngineNumber("ENGINE12345");
        sampleDto.setVehicleType("TRUCK");
        sampleDto.setModel("Signa 2823");
        sampleDto.setManufacturer("Tata");
        sampleDto.setCapacityKg(25000);
        sampleDto.setStatus("AVAILABLE");
        sampleDto.setInsuranceExpiryDate(LocalDate.now().plusYears(1));
        sampleDto.setPollutionExpiryDate(LocalDate.now().plusYears(1));
        sampleDto.setFuelType("DIESEL");

        // Load posting options
        sampleDto.setTruckType("OPEN_TRUCK");
        sampleDto.setBodyType("OPEN_FULL_BODY");
        sampleDto.setTruckLengthFeet(19.0);
        sampleDto.setLengthOption("19 Feet");
        sampleDto.setWheelerCount(14);
        sampleDto.setWheelerOption("14 Wheeler");
        sampleDto.setPassingCapacityTons(24.5);
        sampleDto.setPassingCapacityOption("24-25 Ton");
    }

    @Test
    @DisplayName("createVehicle maps load posting fields correctly")
    void testCreateVehicle() {
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> {
            Vehicle v = invocation.getArgument(0);
            v.setId(1L);
            return v;
        });

        Vehicle created = vehicleService.createVehicle(sampleDto);

        assertThat(created).isNotNull();
        assertThat(created.getRegistrationNumber()).isEqualTo("MH12AB1234");
        assertThat(created.getTruckType()).isEqualTo("OPEN_TRUCK");
        assertThat(created.getBodyType()).isEqualTo("OPEN_FULL_BODY");
        assertThat(created.getTruckLengthFeet()).isEqualTo(19.0);
        assertThat(created.getLengthOption()).isEqualTo("19 Feet");
        assertThat(created.getWheelerCount()).isEqualTo(14);
        assertThat(created.getWheelerOption()).isEqualTo("14 Wheeler");
        assertThat(created.getPassingCapacityTons()).isEqualTo(24.5);
        assertThat(created.getPassingCapacityOption()).isEqualTo("24-25 Ton");

        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("updateVehicle updates load posting fields correctly")
    void testUpdateVehicle() {
        Vehicle existingVehicle = new Vehicle(sampleDto);
        existingVehicle.setId(1L);

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(existingVehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleDto updateDto = new VehicleDto();
        updateDto.setVehicleType("TRUCK");
        updateDto.setModel("Signa 2823");
        updateDto.setManufacturer("Tata");
        updateDto.setCapacityKg(25000);
        updateDto.setStatus("AVAILABLE");
        updateDto.setTruckType("CONTAINER");
        updateDto.setBodyType("CLOSED_CONTAINER");
        updateDto.setTruckLengthFeet(32.0);
        updateDto.setLengthOption("32 Feet SXL");
        updateDto.setWheelerCount(6);
        updateDto.setWheelerOption("6 Wheeler");
        updateDto.setPassingCapacityTons(15.0);
        updateDto.setPassingCapacityOption("15 Ton");

        Vehicle updated = vehicleService.updateVehicle(1L, updateDto);

        assertThat(updated.getTruckType()).isEqualTo("CONTAINER");
        assertThat(updated.getBodyType()).isEqualTo("CLOSED_CONTAINER");
        assertThat(updated.getTruckLengthFeet()).isEqualTo(32.0);
        assertThat(updated.getLengthOption()).isEqualTo("32 Feet SXL");
        assertThat(updated.getWheelerCount()).isEqualTo(6);
        assertThat(updated.getPassingCapacityOption()).isEqualTo("15 Ton");
    }

    @Test
    @DisplayName("getTruckOptions delegates to truckOptionService")
    void testGetTruckOptions() {
        TruckOptionsDto lcvDto = TruckOptionsDto.builder()
                .truckType("LCV")
                .displayName("LCV (Light Commercial Vehicle)")
                .bodyTypes(List.of("LCV_OPEN", "LCV_CONTAINER"))
                .availableLengthsFeet(List.of("14 Feet"))
                .availableWheelers(List.of("4 Wheeler", "6 Wheeler"))
                .availablePassingTons(List.of("1-2 Ton", "5-7.5 Ton"))
                .build();

        when(truckOptionService.getPublicTruckOptions("LCV")).thenReturn(List.of(lcvDto));

        List<TruckOptionsDto> lcvOptions = vehicleService.getTruckOptions("LCV");
        assertThat(lcvOptions).hasSize(1);
        assertThat(lcvOptions.get(0).getBodyTypes()).containsExactly("LCV_OPEN", "LCV_CONTAINER");
        assertThat(lcvOptions.get(0).getAvailableWheelers()).contains("4 Wheeler", "6 Wheeler");
        verify(truckOptionService).getPublicTruckOptions("LCV");
    }
}
