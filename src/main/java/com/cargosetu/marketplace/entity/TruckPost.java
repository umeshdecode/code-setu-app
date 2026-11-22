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
@Table(name = "truck_post", indexes = {
        @Index(name = "idx_truck_post_current_geo", columnList = "currentLat,currentLng"),
        @Index(name = "idx_truck_post_route_from_geo", columnList = "routeFromLat,routeFromLng"),
        @Index(name = "idx_truck_post_route_to_geo", columnList = "routeToLat,routeToLng"),
        @Index(name = "idx_truck_post_status", columnList = "truckStatus")
})
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
    private String currentState;
    private String currentAddress;
    private Double currentLat;
    private Double currentLng;

    private String routeFrom;
    private String routeFromState;
    private Double routeFromLat;
    private Double routeFromLng;
    private String routeTo;
    private String routeToState;
    private Double routeToLat;
    private Double routeToLng;

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

    private java.math.BigDecimal pricePerKm;
    private java.math.BigDecimal minimumPrice;
    /** Lump-sum price for the whole trip on this route (e.g. Ahmedabad → Bangalore for 70,000). */
    private java.math.BigDecimal fixedFreight;
    /** Whether the quoted rate / fixed price is open to negotiation. */
    private Boolean priceNegotiable;
    private LocalDateTime availableFrom;
    private LocalDateTime availableTo;
    private String returnRoute;

    private Double rating;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
