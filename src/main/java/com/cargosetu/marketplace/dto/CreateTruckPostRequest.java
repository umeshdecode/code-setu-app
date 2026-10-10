package com.cargosetu.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.cargosetu.marketplace.enums.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTruckPostRequest {
    @NotNull private Long vehicleId;
    private Long driverId;
    @NotBlank private String currentCity;
    private String currentAddress;
    private Double currentLat;
    private Double currentLng;
    private String routeFrom;
    private String routeTo;
    private List<String> waypoints;
    private String expectedLocation;
    private LocalDateTime expectedEta;
    @NotNull private Double totalCapacity;
    @NotNull private Double availableCapacity;
    @NotNull private CapacityUnit capacityUnit;
    private Double availableVolume;
    private Integer availableSpacePercent;
    private List<CargoType> acceptedLoadTypes;
    @NotNull private TruckAvailabilityStatus truckStatus;
}
