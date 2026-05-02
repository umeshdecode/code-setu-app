package com.cargosetu.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DriverDto {
    private Long id;


    private String name;

    private String phoneNumber;

    private String email;


    private String licenseNumber; // UNIQUE

    private LocalDate licenseExpiryDate;


    private Integer experienceYears;


    private String address;


    private String status; // ACTIVE, INACTIVE, SUSPENDED


    private LocalDate joiningDate;


    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
