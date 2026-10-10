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

import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BookingResponse {
    private Long id;
    private String bookingNumber;
    private Long listingId;
    private String listingNumber;
    private ListingType listingType;
    private Long offerId;
    private Integer shipperCompanyId;
    private String shipperCompanyName;
    private Integer transporterCompanyId;
    private String transporterCompanyName;
    private Long vehicleId;
    private String vehicleNumber;
    private String vehicleType;
    private Long driverId;
    private String driverName;
    private BigDecimal agreedFreight;
    private PaymentTermType paymentTerms;
    private Integer advancePercentage;
    private BookingStatus status;
    private LocalDateTime bookedAt;
    private String originCity;
    private String destinationCity;
    private CargoType cargoType;
    private Double weight;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
