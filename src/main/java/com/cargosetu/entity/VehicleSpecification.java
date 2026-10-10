package com.cargosetu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "vehicle_specifications",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_vehicle_spec_model",
                        columnNames = "vehicle_model_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleSpecification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_model_id", nullable = false, unique = true)
    private VehicleModel vehicleModel;

    // Engine
    private String engineType;
    private Integer engineCylinders;
    private Integer engineDisplacementCc;
    private String fuelType;
    private String emissionNorm;
    private Double mileageKmpl;
    private Integer fuelTankCapacityLitres;
    private Double torqueNm;
    private Double enginePowerHp;
    private Integer engineRpm;
    private Double maxSpeedKmh;
    private Double gradeability;

    // Dimensions
    private Integer turningRadiusMm;
    private Integer wheelbaseMm;
    private Integer deckLengthMm;
    private Integer groundClearanceMm;
    private Integer overallWidthMm;
    private Integer overallHeightMm;
    private Integer overallLengthMm;

    // Weight / loading
    private Integer kerbWeightKg;
    private Integer gvwKg;
    private Integer payloadKg;
    private String gearbox;

    // Axle / brake / suspension
    private String axleConfig;
    private String frontAxle;
    private String rearAxle;
    private String frontSuspension;
    private String rearSuspension;
    private String brakeType;
    private Boolean abs;

    // Body / cabin
    private String chassisType;
    private String cabinType;
    private String bodyOption;
    private Integer seatingCapacity;

    // Tyres
    private Boolean tubelessTyres;
    private String frontTyre;
    private String rearTyre;
    private Integer numberOfTyres;

    // Pricing
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    private LocalDateTime lastSyncedAt;
}
