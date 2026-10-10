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
public class ConversationResponse {
    private Long id;
    private Long listingId;
    private Long bookingId;
    private Integer participant1CompanyId;
    private String participant1Name;
    private Integer participant2CompanyId;
    private String participant2Name;
    private String status;
    private LocalDateTime lastMessageAt;
    private Long unreadCount;
    private String lastMessage;
    private LocalDateTime createdAt;
}
