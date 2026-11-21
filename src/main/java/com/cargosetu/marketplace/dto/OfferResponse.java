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
public class OfferResponse {
    private Long id;
    private Long listingId;
    private String listingNumber;
    private ListingType listingType;
    private ListingStatus listingStatus;
    private Integer listingOwnerCompanyId;
    private String listingOwnerCompanyName;
    private Long listingPostedByStaffId;
    private String originCity;
    private String originState;
    private String destinationCity;
    private String destinationState;
    private BigDecimal listingAskingPrice;
    private CargoType cargoType;
    private Double weight;
    private CapacityUnit weightUnit;
    private Integer bidderCompanyId;
    private String bidderCompanyName;
    private Long offeredByStaffId;
    private String offeredByName;
    private BigDecimal amount;
    private PaymentTermType paymentTerms;
    private LocalDateTime validUntil;
    private OfferStatus status;
    private String message;
    private Long parentOfferId;
    private Long latestCounterOfferId;
    private BigDecimal latestCounterAmount;
    private OfferStatus latestCounterStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
