package com.cargosetu.controller;

import com.cargosetu.dto.DriverDto;
import com.cargosetu.model.Driver;
import com.cargosetu.service.DriverService;
import com.cargosetu.utils.BaseURL;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(BaseURL.BASE_URL+BaseURL.DRIVER)
public class DriverController {

    @Autowired
    private DriverService driverService;

    @PostMapping
    public ResponseEntity<Driver> create(@Valid @RequestBody DriverDto driver) {
        return ResponseEntity.ok(driverService.createDriver(driver));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Driver> get(@PathVariable Long id) {
        return ResponseEntity.ok(driverService.getDriver(id));
    }

    @GetMapping
    public ResponseEntity<List<Driver>> getAll() {
        return ResponseEntity.ok(driverService.getAllDrivers());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Driver> update(@PathVariable Long id, @Valid @RequestBody DriverDto driver) {
        return ResponseEntity.ok(driverService.updateDriver(id, driver));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        driverService.deleteDriver(id);
        return ResponseEntity.ok("Driver deleted");
    }
}
