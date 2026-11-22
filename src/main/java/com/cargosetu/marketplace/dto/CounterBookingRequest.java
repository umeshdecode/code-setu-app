package com.cargosetu.marketplace.dto;

import com.cargosetu.marketplace.enums.PaymentTermType;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CounterBookingRequest {
    @NotNull @Positive
    @JsonAlias({"counterFreight", "amount"})
    private BigDecimal proposedFreight;
    private PaymentTermType paymentTerms;
    @Min(0) @Max(100)
    private Integer advancePercentage;
    private String remarks;
}
