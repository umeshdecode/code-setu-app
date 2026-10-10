package com.cargosetu.marketplace.entity;

import com.cargosetu.marketplace.enums.OfferStatus;
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
@Table(name = "marketplace_offer")
public class MarketplaceOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "listing_id")
    private MarketplaceListing listing;

    private Integer bidderCompanyId;

    private Long offeredByStaffId;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentTermType paymentTerms;

    private LocalDateTime validUntil;

    @Enumerated(EnumType.STRING)
    private OfferStatus status;

    @Column(columnDefinition = "TEXT")
    private String message;

    private Long parentOfferId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
