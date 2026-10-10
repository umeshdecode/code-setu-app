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
public class PaymentTransactionResponse {
    private Long id;
    private Long orderId;
    private String transactionType;
    private BigDecimal amount;
    private String direction;
    private PaymentStatus status;
    private String transactionRef;
    private String gatewayRef;
    private PaymentMode paymentMode;
    private LocalDateTime createdAt;
}
