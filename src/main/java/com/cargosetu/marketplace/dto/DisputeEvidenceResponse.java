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
public class DisputeEvidenceResponse {
    private Long id;
    private Long disputeId;
    private String evidenceType;
    private String fileRef;
    private String description;
    private String uploadedByName;
    private LocalDateTime createdAt;
}
