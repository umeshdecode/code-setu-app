package com.cargosetu.service;

import com.cargosetu.dto.DriverDto;
import com.cargosetu.model.Driver;

import java.util.List;

public interface
DriverService {
    public Driver createDriver(DriverDto driver) ;
    public Driver getDriver(Long id) ;
    public List<Driver> getAllDrivers() ;
    public void deleteDriver(Long id) ;
    public Driver updateDriver(Long id, DriverDto updated) ;
    }
