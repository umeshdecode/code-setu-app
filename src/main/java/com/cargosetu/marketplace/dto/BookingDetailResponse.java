package com.cargosetu.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.cargosetu.marketplace.enums.*;

import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class BookingDetailResponse extends BookingResponse {
    private String cancellationPolicySnapshot;
    private String commercialSnapshot;
    private List<BookingItemResponse> items;
    private List<PaymentOrderResponse> payments;
    private String tripNumber;
    private MarketplaceTripStatus tripStatus;
    private List<RatingResponse> ratings;
}
