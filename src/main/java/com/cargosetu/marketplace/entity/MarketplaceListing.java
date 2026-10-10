package com.cargosetu.marketplace.entity;

import com.cargosetu.marketplace.enums.ListingStatus;
import com.cargosetu.marketplace.enums.ListingType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "marketplace_listing")
public class MarketplaceListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String listingNumber;

    @Enumerated(EnumType.STRING)
    private ListingType type;

    private Integer companyId;

    private Long postedByStaffId;

    @Enumerated(EnumType.STRING)
    private ListingStatus status;

    @Builder.Default
    private String visibility = "PUBLIC";

    private LocalDateTime validFrom;

    private LocalDateTime validUntil;

    @Version
    private Long version;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "listing", cascade = CascadeType.ALL)
    private LoadPost loadPost;

    @OneToOne(mappedBy = "listing", cascade = CascadeType.ALL)
    private TruckPost truckPost;
}
