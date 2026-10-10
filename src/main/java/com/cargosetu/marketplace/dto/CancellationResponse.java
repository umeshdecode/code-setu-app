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
public class CancellationResponse {
    private Long id;
    private Long bookingId;
    private String bookingNumber;
    private Long cancelledByStaffId;
    private String cancelledByName;
    private Integer cancelledByCompanyId;
    private String reasonCode;
    private String reasonText;
    private String stage;
    private BigDecimal refundAmount;
    private BigDecimal penaltyAmount;
    private LocalDateTime createdAt;
}
