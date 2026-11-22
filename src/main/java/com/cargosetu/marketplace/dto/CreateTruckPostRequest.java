package com.cargosetu.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonAlias;
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
    @NotBlank @Size(max = 100) private String currentCity;
    private String currentState;
    private String currentAddress;
    private Double currentLat;
    private Double currentLng;
    private String routeFrom;
    private String routeFromState;
    private Double routeFromLat;
    private Double routeFromLng;
    private String routeTo;
    private String routeToState;
    private Double routeToLat;
    private Double routeToLng;
    private List<String> waypoints;
    private String expectedLocation;
    private LocalDateTime expectedEta;
    @NotNull @Positive private Double totalCapacity;
    @NotNull @PositiveOrZero private Double availableCapacity;
    @NotNull private CapacityUnit capacityUnit;
    @PositiveOrZero private Double availableVolume;
    @Min(0) @Max(100) private Integer availableSpacePercent;
    private List<CargoType> acceptedLoadTypes;
    @NotNull private TruckAvailabilityStatus truckStatus;

    @PositiveOrZero private BigDecimal pricePerKm;
    @PositiveOrZero private BigDecimal minimumPrice;
    /** Lump-sum price for the whole route, as an alternative to a per-km rate. */
    @PositiveOrZero private BigDecimal fixedFreight;
    private Boolean priceNegotiable;
    private LocalDateTime availableFrom;
    private LocalDateTime availableTo;
    @Size(max = 255) private String returnRoute;

    @JsonIgnore
    @AssertTrue(message = "available capacity cannot exceed total capacity")
    public boolean isCapacityConsistent() {
        return totalCapacity == null || availableCapacity == null || availableCapacity <= totalCapacity;
    }

    @JsonIgnore
    @AssertTrue(message = "availability end must be after its start")
    public boolean isAvailabilityWindowValid() {
        return availableFrom == null || availableTo == null || !availableTo.isBefore(availableFrom);
    }

    @JsonIgnore
    @AssertTrue(message = "route start and end must be different")
    public boolean isDistinctRoute() {
        String from = routeFrom != null && !routeFrom.isBlank() ? routeFrom : currentCity;
        return from == null || routeTo == null || routeTo.isBlank()
                || !from.trim().equalsIgnoreCase(routeTo.trim());
    }
}
