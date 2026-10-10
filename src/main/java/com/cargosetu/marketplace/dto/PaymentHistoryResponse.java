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
public class PaymentHistoryResponse {
    private Long bookingId;
    private String bookingNumber;
    private BigDecimal totalFreight;
    private BigDecimal totalPaid;
    private BigDecimal totalPending;
    private BigDecimal totalRefunded;
    private List<PaymentOrderResponse> orders;
}
