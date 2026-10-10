package com.cargosetu.marketplace.entity;

import com.cargosetu.marketplace.enums.PaymentMode;
import com.cargosetu.marketplace.enums.WalletTransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "marketplace_wallet_transaction")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Immutable
public class WalletTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long walletId;

    @Enumerated(EnumType.STRING)
    private WalletTransactionType type;

    private String direction;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String status;
    private String referenceType;
    private Long referenceId;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;

    private String description;
    private Long createdByStaffId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
