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
public class TruckPostSearchRequest {
    private String originCity;
    private String destinationCity;
    private String currentCity;
    private String routeTo;
    private String truckType;
    private Double truckLength;
    private String bodyType;
    private Double minCapacity;
    private CapacityUnit capacityUnit;
    private CargoType cargoType;
    private TruckAvailabilityStatus truckStatus;
    private Double maxDetourKm;
    private Boolean verifiedOnly;
    private Double minRating;
    @Builder.Default private Integer page = 0;
    @Builder.Default private Integer size = 20;
    private String sortBy;
    private String sortDir;
}
