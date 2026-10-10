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
public class LoadPostSearchRequest {
    private String originCity;
    private String destinationCity;
    private LocalDateTime pickupFrom;
    private LocalDateTime pickupTo;
    private LocalDateTime pickupDate;
    private CargoType cargoType;
    private Double minWeight;
    private Double maxWeight;
    private String requiredTruckType;
    private Double requiredTruckLength;
    private String requiredBodyType;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private PaymentTermType paymentTerms;
    private Boolean verifiedOnly;
    @Builder.Default private Integer page = 0;
    @Builder.Default private Integer size = 20;
    @Builder.Default private String sortBy = "createdAt";
    @Builder.Default private String sortDir = "desc";
}
