package com.cargosetu.marketplace.entity;

import com.cargosetu.marketplace.enums.PaymentMode;
import com.cargosetu.marketplace.enums.PaymentStatus;
import com.cargosetu.marketplace.enums.PaymentTermType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "marketplace_payment_order")
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long bookingId;

    private Integer payerCompanyId;

    private Integer payeeCompanyId;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;

    @Enumerated(EnumType.STRING)
    private PaymentTermType paymentTerm;

    private String component;

    private LocalDateTime dueAt;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String providerRef;

    @Column(unique = true)
    private String idempotencyKey;

    private Long createdByStaffId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
