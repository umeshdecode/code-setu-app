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
public class CreateBookingRequest {
    private Long listingId; // optional when supplied in the URL path
    private Long offerId;
    private Long vehicleId;
    private Long driverId;
    @NotNull @Positive private BigDecimal agreedFreight;
    @NotNull private PaymentTermType paymentTerms;
    @Min(0) @Max(100) private Integer advancePercentage;
    @Positive private Double requestedCapacity;
    @Size(max = 1000) private String remarks;
}
