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
@Table(name = "marketplace_cancellation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cancellation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long bookingId;
    private Long cancelledByStaffId;
    private Integer cancelledByCompanyId;
    private String reasonCode;

    @Column(columnDefinition = "TEXT")
    private String reasonText;

    private String stage;
    private BigDecimal refundAmount;
    private BigDecimal penaltyAmount;

    @Column(columnDefinition = "TEXT")
    private String policySnapshot;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
