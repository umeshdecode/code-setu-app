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
public class CreateLoadPostRequest {
    @NotBlank @Size(max = 100) private String originCity;
    private String originAddress;
    private String originState;
    private String originPincode;
    private Double originLat;
    private Double originLng;
    @NotBlank @Size(max = 100) private String destinationCity;
    private String destinationAddress;
    private String destinationState;
    private String destinationPincode;
    private Double destinationLat;
    private Double destinationLng;
    @NotNull private LocalDateTime pickupFrom;
    private LocalDateTime pickupTo;
    private LocalDateTime deliveryFrom;
    private LocalDateTime deliveryTo;
    @NotNull private CargoType cargoType;
    private String cargoDescription;
    @NotNull @Positive private Double weight;
    @NotNull private CapacityUnit weightUnit;
    @PositiveOrZero private Double volume;
    @PositiveOrZero private Integer packageCount;
    private String requiredTruckType;
    private Double requiredTruckLength;
    private String requiredBodyType;
    private String priceType;
    @PositiveOrZero private BigDecimal askingPrice;
    private PaymentTermType paymentTerms;
    @Size(max = 2000) private String specialInstructions;
    @PositiveOrZero private BigDecimal declaredValue;

    @Min(0) @Max(100)
    private Integer advancePercentage;

    @JsonIgnore
    @AssertTrue(message = "origin and destination must be different")
    public boolean isDistinctRoute() {
        return originCity == null || destinationCity == null
                || !originCity.trim().equalsIgnoreCase(destinationCity.trim());
    }

    @JsonIgnore
    @AssertTrue(message = "pickup date cannot be in the past")
    public boolean isPickupNotPast() {
        return pickupFrom == null || !pickupFrom.toLocalDate().isBefore(java.time.LocalDate.now());
    }

    @JsonIgnore
    @AssertTrue(message = "pickup window end must be after its start")
    public boolean isPickupWindowValid() {
        return pickupFrom == null || pickupTo == null || !pickupTo.isBefore(pickupFrom);
    }

    @JsonIgnore
    @AssertTrue(message = "delivery cannot be before pickup")
    public boolean isDeliveryAfterPickup() {
        LocalDateTime delivery = deliveryTo != null ? deliveryTo : deliveryFrom;
        return pickupFrom == null || delivery == null || !delivery.toLocalDate().isBefore(pickupFrom.toLocalDate());
    }
}
