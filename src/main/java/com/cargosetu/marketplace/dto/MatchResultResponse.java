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
public class MatchResultResponse {
    private Long listingId;
    private String listingNumber;
    private ListingType listingType;
    private Double matchScore;
    private MatchScoreBreakdown scoreBreakdown;
    private LoadPostResponse loadPost;
    private TruckPostResponse truckPost;
}
