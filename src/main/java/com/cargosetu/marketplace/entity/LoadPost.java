package com.cargosetu.marketplace.entity;

import com.cargosetu.marketplace.enums.CapacityUnit;
import com.cargosetu.marketplace.enums.CargoType;
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
@Table(name = "load_post")
public class LoadPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "listing_id")
    private MarketplaceListing listing;

    private String originCity;
    private String originAddress;
    private String originState;
    private String originPincode;
    private Double originLat;
    private Double originLng;

    private String destinationCity;
    private String destinationAddress;
    private String destinationState;
    private String destinationPincode;
    private Double destinationLat;
    private Double destinationLng;

    private LocalDateTime pickupFrom;
    private LocalDateTime pickupTo;
    private LocalDateTime deliveryFrom;
    private LocalDateTime deliveryTo;

    @Enumerated(EnumType.STRING)
    private CargoType cargoType;

    private String cargoDescription;

    private Double weight;

    @Enumerated(EnumType.STRING)
    private CapacityUnit weightUnit;

    private Double volume;

    private Integer packageCount;

    private String requiredTruckType;

    private Double requiredTruckLength;

    private String requiredBodyType;

    private String priceType;

    private BigDecimal askingPrice;

    @Enumerated(EnumType.STRING)
    private PaymentTermType paymentTerms;

    @Column(columnDefinition = "TEXT")
    private String specialInstructions;

    private BigDecimal declaredValue;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
