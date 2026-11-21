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
public class CreateBookingRequest {
    @NotNull private Long listingId;
    private Long offerId;
    private Long vehicleId;
    private Long driverId;
    @NotNull private BigDecimal agreedFreight;
    @NotNull private PaymentTermType paymentTerms;
    private Integer advancePercentage;
    private Double requestedCapacity;
    private String remarks;
}
