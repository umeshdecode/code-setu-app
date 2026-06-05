package com.cargosetu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "driver_vehicle_assignments")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DriverVehicleAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Driver is mandatory")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @NotNull(message = "Vehicle is mandatory")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @NotNull(message = "Assignment timestamp is required")
    @PastOrPresent(message = "Assignment time cannot be in the future")
    @CreationTimestamp
    private LocalDateTime assignedAt;

    private LocalDateTime unassignedAt;

    @NotNull(message = "Active status must be specified")
    private Boolean active = true; // Default to true for new assignments

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
