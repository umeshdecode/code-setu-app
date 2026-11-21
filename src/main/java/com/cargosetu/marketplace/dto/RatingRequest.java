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
public class RatingRequest {
    @NotNull @Min(1) @Max(5) private Integer overallRating;
    private Integer communicationRating;
    private Integer punctualityRating;
    private Integer professionalismRating;
    private Integer paymentRating;
    private List<String> tags;
    private String comment;
}
