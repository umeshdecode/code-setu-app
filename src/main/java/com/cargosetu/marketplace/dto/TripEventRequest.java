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
public class TripEventRequest {
    @NotBlank private String eventType;
    private String location;
    private Double lat;
    private Double lng;
    private String remarks;
    private MarketplaceTripStatus newStatus;
    private String evidenceFileRef;
}
