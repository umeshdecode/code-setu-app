package com.cargosetu.marketplace.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "marketplace_cash_collection")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashCollection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long bookingId;
    private Long tripId;
    private Integer payerCompanyId;
    private String receiverType;
    private Long receiverId;
    private BigDecimal amount;
    private String paymentStage;
    private String receiptNo;
    private String proofDocument;
    private String status;
    private LocalDateTime collectedAt;
    private LocalDateTime verifiedAt;
    private Long verifiedByStaffId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
