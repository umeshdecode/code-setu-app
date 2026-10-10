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
public class TruckPostResponse {
    private Long id;
    private Long listingId;
    private String listingNumber;
    private ListingStatus status;
    private Integer companyId;
    private Long postedByStaffId;
    private String companyName;
    private String vehicleNumber;
    private String vehicleType;
    private String truckType;
    private String bodyType;
    private Double truckLengthFeet;
    private Double passingCapacityTons;
    private String driverName;
    private String driverPhone;
    private String verificationStatus;
    private Double overallRating;
    private Long completedTrips;
    private Double occupiedCapacity;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long vehicleId;
    private Long driverId;
    private String currentCity;
    private String currentAddress;
    private Double currentLat;
    private Double currentLng;
    private String routeFrom;
    private String routeTo;
    private List<String> waypoints;
    private String expectedLocation;
    private LocalDateTime expectedEta;
    private Double totalCapacity;
    private Double availableCapacity;
    private CapacityUnit capacityUnit;
    private Double availableVolume;
    private Integer availableSpacePercent;
    private List<CargoType> acceptedLoadTypes;
    private TruckAvailabilityStatus truckStatus;
}
