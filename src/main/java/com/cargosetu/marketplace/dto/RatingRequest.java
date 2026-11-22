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
public class RatingRequest {
    @NotNull @Min(1) @Max(5) private Integer overallRating;
    @Min(1) @Max(5) private Integer communicationRating;
    @Min(1) @Max(5) private Integer punctualityRating;
    @Min(1) @Max(5) private Integer professionalismRating;
    @Min(1) @Max(5) private Integer paymentRating;
    private List<String> tags;
    @Size(max = 2000) private String comment;
}
