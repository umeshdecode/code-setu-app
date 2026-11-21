package com.cargosetu.marketplace.entity;

import com.cargosetu.marketplace.enums.BookingStatus;
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
@Table(name = "marketplace_booking")
public class MarketplaceBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String bookingNumber;

    @ManyToOne
    @JoinColumn(name = "listing_id")
    private MarketplaceListing listing;

    private Long offerId;

    private Integer shipperCompanyId;

    private Integer transporterCompanyId;

    private Integer requestedByCompanyId;

    private Long requestedByStaffId;

    private Double requestedCapacity;

    private Long vehicleId;

    private Long driverId;

    private BigDecimal proposedFreight;

    private BigDecimal agreedFreight;

    private BigDecimal counterFreight;

    private Integer lastActionByCompanyId;

    @Enumerated(EnumType.STRING)
    private PaymentTermType paymentTerms;

    private Integer advancePercentage;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    private LocalDateTime bookedAt;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(columnDefinition = "TEXT")
    private String cancellationPolicySnapshot;

    @Column(columnDefinition = "TEXT")
    private String commercialSnapshot;

    @Version
    private Long version;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
