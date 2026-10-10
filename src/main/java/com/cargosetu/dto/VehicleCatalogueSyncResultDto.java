package com.cargosetu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCatalogueSyncResultDto {
    private String brand;
    private int pagesProcessed;
    private int productsProcessed;
    private int inserted;
    private int updated;
    private int failed;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
