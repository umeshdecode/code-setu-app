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
public class MarketplaceDashboardResponse {
    private Long activeLoads;
    private Long activeTrucks;
    private Long activeBookings;
    private Long activeTrips;
    private Long pendingPayments;
    private Long openDisputes;
    private Long totalCompletedBookings;
    private BigDecimal totalRevenue;
}
