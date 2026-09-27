package com.cargosetu.controller;

import com.cargosetu.model.VehicleManufacturerModel;
import com.cargosetu.repo.VehicleManufacturerModelRepository;
import com.cargosetu.utils.BaseURL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(BaseURL.VEHICLE + BaseURL.MANUFACTURER_MODELS)
public class VehicleManufacturerModelController {

    @Autowired
    private VehicleManufacturerModelRepository repository;

    @GetMapping
    public ResponseEntity<List<VehicleManufacturerModel>> getAll() {
        return ResponseEntity.ok(repository.findAllByOrderByManufacturerAscModelAsc());
    }

    @GetMapping("/manufacturers")
    public ResponseEntity<List<String>> getManufacturers() {
        return ResponseEntity.ok(repository.findDistinctManufacturers());
    }

    @GetMapping("/manufacturers/{manufacturer}/models")
    public ResponseEntity<List<VehicleManufacturerModel>> getModelsForManufacturer(@PathVariable String manufacturer) {
        return ResponseEntity.ok(repository.findByManufacturerOrderByModel(manufacturer));
    }
}
