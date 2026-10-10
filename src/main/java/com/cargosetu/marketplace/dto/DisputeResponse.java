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
public class DisputeResponse {
    private Long id;
    private Long bookingId;
    private String bookingNumber;
    private Long raisedByStaffId;
    private String raisedByName;
    private Integer raisedByCompanyId;
    private String raisedByCompanyName;
    private DisputeCategory category;
    private String description;
    private String priority;
    private DisputeStatus status;
    private String resolution;
    private String resolvedByName;
    private LocalDateTime resolvedAt;
    private List<DisputeEvidenceResponse> evidences;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
