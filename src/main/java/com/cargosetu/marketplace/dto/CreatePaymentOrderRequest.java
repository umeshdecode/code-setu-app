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
public class CreatePaymentOrderRequest {
    @NotNull private BigDecimal amount;
    private PaymentMode paymentMode;
    private PaymentTermType paymentTerm;
    private String component;
    @NotBlank private String idempotencyKey;
}
