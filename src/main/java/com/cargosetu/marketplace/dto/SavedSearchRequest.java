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
public class SavedSearchRequest {
    @NotBlank private String searchType;
    private String originCity;
    private String destinationCity;
    private String truckType;
    private Double truckLength;
    private CargoType cargoType;
    private Double minCapacity;
    private BigDecimal maxPrice;
}
