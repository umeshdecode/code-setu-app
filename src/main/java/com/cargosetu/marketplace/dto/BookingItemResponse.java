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
public class BookingItemResponse {
    private Long id;
    private Long bookingId;
    private Double loadWeight;
    private Double allocatedCapacity;
    private CapacityUnit capacityUnit;
    private Integer packageCount;
    private CargoType cargoType;
    private String cargoDescription;
    private BigDecimal declaredValue;
}
