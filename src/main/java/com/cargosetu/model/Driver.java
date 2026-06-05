package com.cargosetu.model;

import com.cargosetu.dto.DriverDto;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
public class Driver {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is mandatory")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Phone number is mandatory")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
    private String phoneNumber;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is mandatory")
    private String email;

    @NotBlank(message = "License number is mandatory")
    @Column(unique = true)
    private String licenseNumber;

    @NotNull(message = "License expiry date is mandatory")
    @Future(message = "License expiry date must be in the future")
    private LocalDate licenseExpiryDate;

    @Min(value = 0, message = "Experience cannot be negative")
    @Max(value = 60, message = "Experience years seems unrealistic")
    private Integer experienceYears;

    @Size(max = 255)
    private String address;

    @Pattern(regexp = "ACTIVE|INACTIVE|SUSPENDED", message = "Status must be ACTIVE, INACTIVE, or SUSPENDED")
    private String status;

    @PastOrPresent(message = "Joining date cannot be in the future")
    private LocalDate joiningDate;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Driver(DriverDto dto) {
        if (dto == null) {
            return;
        }
        this.id = dto.getId();
        this.name = dto.getName();
        this.phoneNumber = dto.getPhoneNumber();
        this.email = dto.getEmail();
        this.licenseNumber = dto.getLicenseNumber();
        this.licenseExpiryDate = dto.getLicenseExpiryDate();
        this.experienceYears = dto.getExperienceYears();
        this.address = dto.getAddress();
        this.status = dto.getStatus();
        this.joiningDate = dto.getJoiningDate();
    }

    public Driver() {

    }
}
