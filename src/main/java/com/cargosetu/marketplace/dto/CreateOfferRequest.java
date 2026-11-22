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
public class CreateOfferRequest {
    @NotNull @Positive private BigDecimal amount;
    private PaymentTermType paymentTerms;
    private LocalDateTime validUntil;
    @Size(max = 1000) private String message;

    @Min(0) @Max(100)
    private Integer advancePercentage;

    @JsonIgnore
    @AssertTrue(message = "offer validity must be in the future")
    public boolean isValidUntilFuture() {
        return validUntil == null || validUntil.isAfter(LocalDateTime.now());
    }
}
