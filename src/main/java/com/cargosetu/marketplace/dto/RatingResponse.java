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
public class RatingResponse {
    private Long id;
    private Long bookingId;
    private Integer reviewerCompanyId;
    private String reviewerCompanyName;
    private Long reviewerStaffId;
    private String reviewerName;
    private Integer revieweeCompanyId;
    private String revieweeCompanyName;
    private Integer overallRating;
    private Integer communicationRating;
    private Integer punctualityRating;
    private Integer professionalismRating;
    private Integer paymentRating;
    private List<String> tags;
    private String comment;
    private LocalDateTime createdAt;
}
