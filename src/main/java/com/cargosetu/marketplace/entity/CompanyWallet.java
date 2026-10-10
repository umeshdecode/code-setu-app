package com.cargosetu.marketplace.entity;

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
@Table(name = "marketplace_company_wallet")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyWallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Integer companyId;

    @Builder.Default
    private String currency = "INR";

    @Builder.Default
    private String status = "ACTIVE";

    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal pendingBalance = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal heldBalance = BigDecimal.ZERO;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
