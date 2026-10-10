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
public class TripEventResponse {
    private Long id;
    private Long tripId;
    private String eventType;
    private LocalDateTime eventTime;
    private String location;
    private Double lat;
    private Double lng;
    private String actorName;
    private String remarks;
    private String evidenceFileRef;
    private String previousStatus;
    private String newStatus;
    private LocalDateTime createdAt;
}
