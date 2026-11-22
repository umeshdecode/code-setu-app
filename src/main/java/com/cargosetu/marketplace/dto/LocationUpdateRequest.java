package com.cargosetu.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonAlias;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.cargosetu.marketplace.enums.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationUpdateRequest {
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") private Double lat;
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") private Double lng;
    private Float accuracy;
    private String source;
}
