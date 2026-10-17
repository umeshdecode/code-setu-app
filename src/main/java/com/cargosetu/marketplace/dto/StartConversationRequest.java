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
public class StartConversationRequest {
    private Long listingId;
    private Long bookingId;
    @NotNull private Integer participantCompanyId;
    private Long participantStaffId;
    private String initialMessage;
}
