package com.cargosetu.controller;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.dto.DriverDto;
import com.cargosetu.model.Driver;
import com.cargosetu.repo.DriverRepository;
import com.cargosetu.service.DriverService;
import com.cargosetu.utils.BaseURL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Objects;

/**
 * Drivers are company-scoped: every call uses the company of the logged-in user, so a company
 * only ever sees, creates, edits or deletes its own drivers.
 */
@RestController
@RequestMapping(BaseURL.BASE_URL+BaseURL.DRIVER)
public class DriverController {

    @Autowired
    private DriverService driverService;

    @Autowired
    private DriverRepository driverRepository;

    @PostMapping
    public ResponseEntity<Driver> create(@AuthenticationPrincipal LoggedInUser user, @RequestBody DriverDto driver) {
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Driver created = driverService.createDriver(driver);
        created.setCompanyId(user.getCompanyId());
        return ResponseEntity.ok(driverRepository.save(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Driver> get(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id) {
        Driver driver = driverRepository.findById(id).orElse(null);
        if (!ownedBy(driver, user)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(driver);
    }

    /** Only the caller's company's drivers (was: every driver of every company). */
    @GetMapping
    public ResponseEntity<List<Driver>> getAll(@AuthenticationPrincipal LoggedInUser user) {
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(driverService.getDriversByCompany(user.getCompanyId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Driver> update(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id, @RequestBody DriverDto driver) {
        if (!ownedBy(driverRepository.findById(id).orElse(null), user)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(driverService.updateDriver(id, driver));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id) {
        if (!ownedBy(driverRepository.findById(id).orElse(null), user)) {
            return ResponseEntity.notFound().build();
        }
        driverService.deleteDriver(id);
        return ResponseEntity.ok("Driver deleted");
    }

    private static boolean ownedBy(Driver driver, LoggedInUser user) {
        return driver != null && user != null && user.getCompanyId() != null
                && Objects.equals(driver.getCompanyId(), user.getCompanyId());
    }
}
