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
public class MatchScoreBreakdown {
    private Double routeScore;
    private Double dateScore;
    private Double truckScore;
    private Double capacityScore;
    private Double cargoScore;
    private Double locationScore;
    private Double trustScore;
}
