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
public class PaymentOrderResponse {
    private Long id;
    private Long bookingId;
    private String bookingNumber;
    private Integer payerCompanyId;
    private String payerCompanyName;
    private Integer payeeCompanyId;
    private String payeeCompanyName;
    private BigDecimal amount;
    private PaymentMode paymentMode;
    private PaymentTermType paymentTerm;
    private String component;
    private LocalDateTime dueAt;
    private PaymentStatus status;
    private String providerRef;
    private List<PaymentTransactionResponse> transactions;
    private LocalDateTime createdAt;
}
