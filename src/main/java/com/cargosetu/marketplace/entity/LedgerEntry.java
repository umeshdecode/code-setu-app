package com.cargosetu.marketplace.entity;

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
@Table(name = "marketplace_ledger_entry")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Immutable
public class LedgerEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer companyId;
    private Long bookingId;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal balanceAfter;
    private String referenceType;
    private Long referenceId;
    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
