package com.cargosetu.marketplace.dto;

import com.cargosetu.marketplace.enums.PaymentTermType;
import jakarta.validation.constraints.NotNull;
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
    @NotNull
    private BigDecimal proposedFreight;
    private PaymentTermType paymentTerms;
    private Integer advancePercentage;
    private String remarks;
}
