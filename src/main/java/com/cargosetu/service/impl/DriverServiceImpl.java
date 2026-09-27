package com.cargosetu.service.impl;

import com.cargosetu.dto.DriverDto;
import com.cargosetu.model.Driver;
import com.cargosetu.notification.event.GenericNotificationEvent;
import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.repo.DriverRepository;
import com.cargosetu.service.DriverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DriverServiceImpl implements DriverService {
    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public Driver createDriver(DriverDto driverdto) {
        try {
            log.info("Creating driver with license: {}", driverdto.getLicenseNumber());
            Driver driver = new Driver(driverdto);
            driver.setCreatedAt(LocalDateTime.now());
            driver.setUpdatedAt(LocalDateTime.now());
            Driver saved = driverRepository.save(driver);
            publishDriverOnboardedNotification(saved);
            return saved;

        } catch (Exception e) {
            log.error("Error creating driver: {}", e.getMessage(), e);
            throw new RuntimeException("Driver creation failed");
        }
    }

    private void publishDriverOnboardedNotification(Driver driver) {
        if (driver.getEmail() == null || driver.getEmail().isBlank()) {
            log.warn("Skipping DRIVER_ONBOARDED notification for driver {} - no email on file", driver.getId());
            return;
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("driverName", driver.getName());
        variables.put("driverCode", "CS-DRV-" + String.format("%04d", driver.getId()));
        variables.put("mobileNumber", driver.getPhoneNumber());
        variables.put("licenseNumber", driver.getLicenseNumber());
        variables.put("licenseExpiryDate", driver.getLicenseExpiryDate() != null ? driver.getLicenseExpiryDate().toString() : "");
        variables.put("viewProfileUrl", frontendUrl + "/drivers/" + driver.getId());
        // No vehicle relation exists on Driver at creation time (assignment is a separate
        // step via DriverVehicleAssignmentService), so this section is always left empty.
        variables.put("vehicleSection", "");

        eventPublisher.publishEvent(new GenericNotificationEvent(
                "DRIVER_ONBOARDED",
                List.of(driver.getEmail()),
                NotificationChannel.EMAIL,
                variables,
                driver.getId().toString()
        ));
    }

    @Override
    public Driver getDriver(Long id) {
        try {
            log.info("Fetching driver id: {}", id);

            return driverRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Driver not found"));

        } catch (Exception e) {
            log.error("Error fetching driver {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Driver> getAllDrivers() {
        try {
            log.info("Fetching all drivers");
            return driverRepository.findAll();
        } catch (Exception e) {
            log.error("Error fetching drivers: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Driver updateDriver(Long id, DriverDto updated) {
        try {
            log.info("Updating driver id: {}", id);

            Driver driver = getDriver(id);

            driver.setName(updated.getName());
            driver.setPhoneNumber(updated.getPhoneNumber());
            driver.setEmail(updated.getEmail());
            driver.setStatus(updated.getStatus());
            driver.setLicenseExpiryDate(updated.getLicenseExpiryDate());
            driver.setUpdatedAt(LocalDateTime.now());

            return driverRepository.save(driver);

        } catch (Exception e) {
            log.error("Error updating driver {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void deleteDriver(Long id) {
        try {
            log.info("Deleting driver id: {}", id);

            Driver driver = getDriver(id);
            driverRepository.delete(driver);

        } catch (Exception e) {
            log.error("Error deleting driver {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}
