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
public class CreateLoadPostRequest {
    @NotBlank private String originCity;
    private String originAddress;
    private String originState;
    private String originPincode;
    private Double originLat;
    private Double originLng;
    @NotBlank private String destinationCity;
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
    @NotNull private Double weight;
    @NotNull private CapacityUnit weightUnit;
    private Double volume;
    private Integer packageCount;
    private String requiredTruckType;
    private Double requiredTruckLength;
    private String requiredBodyType;
    private String priceType;
    private BigDecimal askingPrice;
    private PaymentTermType paymentTerms;
    private String specialInstructions;
    private BigDecimal declaredValue;
}
