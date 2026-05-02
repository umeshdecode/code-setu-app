package com.cargosetu.service.impl;

import com.cargosetu.dto.DriverDto;
import com.cargosetu.model.Driver;
import com.cargosetu.repo.DriverRepository;
import com.cargosetu.service.DriverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class DriverServiceImpl implements DriverService {
    @Autowired
    private DriverRepository driverRepository;

    @Override
    public Driver createDriver(DriverDto driverdto) {
        try {
            log.info("Creating driver with license: {}", driverdto.getLicenseNumber());
            Driver driver = new Driver(driverdto);
            driver.setCreatedAt(LocalDateTime.now());
            driver.setUpdatedAt(LocalDateTime.now());
            return driverRepository.save(driver);

        } catch (Exception e) {
            log.error("Error creating driver: {}", e.getMessage(), e);
            throw new RuntimeException("Driver creation failed");
        }
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
