package com.cargosetu.marketplace.entity;

import com.cargosetu.marketplace.enums.MarketplaceTripStatus;
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
@Table(name = "marketplace_trip")
public class MarketplaceTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String tripNumber;

    private Long bookingId;

    private Long existingTripId;

    private Long vehicleId;

    private Long driverId;

    private String startCity;
    private String startAddress;
    private Double startLat;
    private Double startLng;

    private String endCity;
    private String endAddress;
    private Double endLat;
    private Double endLng;

    private LocalDateTime plannedStartTime;
    private LocalDateTime plannedEndTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;

    @Enumerated(EnumType.STRING)
    private MarketplaceTripStatus status;

    private Integer companyId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
