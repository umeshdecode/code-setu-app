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
public class WalletTransactionResponse {
    private Long id;
    private WalletTransactionType type;
    private String direction;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String status;
    private String referenceType;
    private Long referenceId;
    private PaymentMode paymentMode;
    private String description;
    private String createdByName;
    private LocalDateTime createdAt;
}
