package com.cargosetu.service.impl;

import com.cargosetu.dto.DriverDto;
import com.cargosetu.model.Driver;
import com.cargosetu.notification.event.GenericNotificationEvent;
import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.repo.DriverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceImplTest {

    @Mock
    private DriverRepository driverRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DriverServiceImpl driverService;

    @BeforeEach
    void setUp() {
        driverService = new DriverServiceImpl();
        ReflectionTestUtils.setField(driverService, "driverRepository", driverRepository);
        ReflectionTestUtils.setField(driverService, "eventPublisher", eventPublisher);
        ReflectionTestUtils.setField(driverService, "frontendUrl", "http://localhost:4200");
    }

    private DriverDto sampleDto() {
        DriverDto dto = new DriverDto();
        dto.setName("Rakesh Kumar");
        dto.setPhoneNumber("+919328334187");
        dto.setEmail("rakesh.kumar93@gmail.com");
        dto.setLicenseNumber("BVCHDSU851AD");
        dto.setLicenseExpiryDate(LocalDate.of(2026, 8, 29));
        dto.setExperienceYears(5);
        dto.setStatus("AVAILABLE");
        return dto;
    }

    @Test
    void createDriver_publishesDriverOnboardedEvent() {
        DriverDto dto = sampleDto();
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> {
            Driver d = inv.getArgument(0);
            d.setId(7L);
            return d;
        });

        driverService.createDriver(dto);

        ArgumentCaptor<GenericNotificationEvent> captor = ArgumentCaptor.forClass(GenericNotificationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        GenericNotificationEvent event = captor.getValue();
        assertThat(event.templateCode()).isEqualTo("DRIVER_ONBOARDED");
        assertThat(event.recipients()).containsExactly("rakesh.kumar93@gmail.com");
        assertThat(event.channel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(event.referenceId()).isEqualTo("7");
        assertThat(event.variables())
                .containsEntry("driverName", "Rakesh Kumar")
                .containsEntry("driverCode", "CS-DRV-0007")
                .containsEntry("mobileNumber", "+919328334187")
                .containsEntry("licenseNumber", "BVCHDSU851AD")
                .containsEntry("licenseExpiryDate", "2026-08-29")
                .containsEntry("viewProfileUrl", "http://localhost:4200/drivers/7")
                .containsEntry("vehicleSection", "");
    }

    @Test
    void createDriver_noEmail_doesNotPublishEvent() {
        DriverDto dto = sampleDto();
        dto.setEmail(null);
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> {
            Driver d = inv.getArgument(0);
            d.setId(8L);
            return d;
        });

        driverService.createDriver(dto);

        verifyNoInteractions(eventPublisher);
    }
}
