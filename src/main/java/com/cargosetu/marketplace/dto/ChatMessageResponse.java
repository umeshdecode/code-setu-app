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
public class ChatMessageResponse {
    private Long id;
    private Long conversationId;
    private Long senderStaffId;
    private String senderName;
    private String senderStaffName;
    private Integer senderCompanyId;
    private String senderCompanyName;
    private String messageType;
    private String message;
    private String attachmentRef;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
}
