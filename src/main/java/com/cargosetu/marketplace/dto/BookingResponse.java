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
    private Integer listingOwnerCompanyId;
    private Long listingPostedByStaffId;
    private Long offerId;
    private Integer shipperCompanyId;
    private String shipperCompanyName;
    private Integer transporterCompanyId;
    private String transporterCompanyName;
    private Integer requestedByCompanyId;
    private Long requestedByStaffId;
    private Double requestedCapacity;
    private CapacityUnit capacityUnit;
    private Long vehicleId;
    private String vehicleNumber;
    private String vehicleType;
    private Long driverId;
    private String driverName;
    private BigDecimal proposedFreight;
    private BigDecimal agreedFreight;
    private BigDecimal counterFreight;
    private Integer lastActionByCompanyId;
    private PaymentTermType paymentTerms;
    private Integer advancePercentage;
    private BookingStatus status;
    private LocalDateTime bookedAt;
    private String rejectionReason;
    private String remarks;
    private String originCity;
    private String originState;
    private String destinationCity;
    private String destinationState;
    private CargoType cargoType;
    private Double weight;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
