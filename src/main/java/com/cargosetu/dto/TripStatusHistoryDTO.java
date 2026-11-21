package com.cargosetu.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TripStatusHistoryDTO {
    private Long id;
    private Long tripId;
    private String oldStatus;
    private String newStatus;
    private String changedBy;
    private LocalDateTime changedAt;
}
