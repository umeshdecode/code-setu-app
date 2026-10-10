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
public class OfferResponse {
    private Long id;
    private Long listingId;
    private String listingNumber;
    private Integer bidderCompanyId;
    private String bidderCompanyName;
    private Long offeredByStaffId;
    private String offeredByName;
    private BigDecimal amount;
    private PaymentTermType paymentTerms;
    private LocalDateTime validUntil;
    private OfferStatus status;
    private String message;
    private Long parentOfferId;
    private LocalDateTime createdAt;
}
