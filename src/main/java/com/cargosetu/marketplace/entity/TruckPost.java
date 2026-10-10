package com.cargosetu.marketplace.entity;

import com.cargosetu.marketplace.enums.CapacityUnit;
import com.cargosetu.marketplace.enums.TruckAvailabilityStatus;
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
@Table(name = "truck_post")
public class TruckPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "listing_id")
    private MarketplaceListing listing;

    private Long vehicleId;

    private Long driverId;

    private String currentCity;
    private String currentAddress;
    private Double currentLat;
    private Double currentLng;

    private String routeFrom;
    private String routeTo;

    @Column(columnDefinition = "TEXT")
    private String waypoints;

    private String expectedLocation;
    private LocalDateTime expectedEta;

    private Double totalCapacity;
    private Double availableCapacity;
    private Double occupiedCapacity;

    @Enumerated(EnumType.STRING)
    private CapacityUnit capacityUnit;

    private Double availableVolume;
    private Integer availableSpacePercent;

    @Column(columnDefinition = "TEXT")
    private String acceptedLoadTypes;

    @Enumerated(EnumType.STRING)
    private TruckAvailabilityStatus truckStatus;

    @Column(name = "is_verified")
    private Boolean isVerified;

    private Double rating;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
